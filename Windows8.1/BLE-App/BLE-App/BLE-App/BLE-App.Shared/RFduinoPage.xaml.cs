using BlueConnect;
using Sensors;
using System;
using System.Collections.Generic;
using System.Linq;
using System.Text;
using System.Threading.Tasks;
using Windows.Devices.Bluetooth;
using Windows.Devices.Bluetooth.GenericAttributeProfile;
using Windows.Devices.Enumeration;
using Windows.Networking;
using Windows.Networking.Sockets;
using Windows.Storage.Streams;
using Windows.UI.Popups;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;

/*******Sources*********
BLE:
http://forum.rfduino.com/index.php?topic=349.0
http://talkingaboutit.azurewebsites.net/post/windows-phone-8-1-ble-communications-part-2 (Part 1 interesting for finding out UUID's)
Wlan:
http://metronuggets.com/2013/03/18/how-to-send-and-receive-a-udp-broadcast-in-windows-phone-8-and-win8/
Sensor:
https://code.msdn.microsoft.com/universal-windows-app-cb3248c3 (OrientationSensor Sample)
 */


namespace RfduinoBleApp
{
    /// <summary>
    /// Main page for the RFduino-receiver app
    /// </summary>
    public sealed partial class RFduinoPage : Page
    {
        /// <summary>
        /// The delay between two wlan messages
        /// </summary>
        private const int SEND_WLAN_DELAY = 10;
        /// <summary>
        /// The ID for the orientation sensor of the receiver.
        /// </summary>
        private const UInt32 ORIENTATION_SENSOR_ID = 10;
        /// <summary>
        /// The ID of the RFDuino
        /// NOTE: currently just one Device supported
        /// </summary>
        private const UInt32 BLE_ID = 11;
        /// <summary>
        /// Default IP address to send to
        /// </summary>
        private string wlanHost = "192.168.0.255";
        /// <summary>
        /// Port to send to
        /// </summary>
        private string wlanPort = "5050";

        /// <summary>
        /// Sensormanager
        /// </summary>
        private SensorManager sensor;
        /// <summary>
        /// Dictionary for quaternions (ID -> quaternion data)
        /// </summary>
        Dictionary<UInt32, Quaternion> quaternions = new Dictionary<uint, Quaternion>();

        #region rfduino device
        /// <summary>
        /// BLE device uid for RFDuino
        /// </summary>
        static readonly Guid RFduinoDeviceId = new Guid("00002220-0000-1000-8000-00805f9b34fb");
        /// <summary>
        /// Service ID for reading
        /// </summary>
        static readonly Guid RFduinoReaderServiceId = GattCharacteristic.ConvertShortIdToUuid(0x2221);

        DeviceInformation rFduinoDevice;
        GattDeviceService rFduinoService;
        GattCharacteristic readerChars;
        #endregion

        /// <summary>
        /// Time between display updates
        /// </summary>
        private const int DISPLAY_INTERVAL = 30;
        /// <summary>
        ///  Timer for display updates
        /// </summary>
        DispatcherTimer displayTimer;

        /// <summary>
        /// Counts the number of messages
        /// </summary>
        int numOfMessages = 0;

        public RFduinoPage()
        {
            this.InitializeComponent();

            sensor = new SensorManager();

            //initialize Dispatcher to display quaternions
            displayTimer = new DispatcherTimer();
            displayTimer.Tick += Display;
            displayTimer.Interval = new TimeSpan(0, 0, 0, 0, DISPLAY_INTERVAL);
            displayTimer.Start();

            //Own sensors
            sensor.SensorReadingOrientation += SensorReadingOrientation;

            hostNameBox.Text = wlanHost;
            portNameBox.Text = wlanPort;
        }

        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            // Called when the page becomes active
        }
        /// <summary>
        /// Stops everything
        /// </summary>
        /// <param name="e"></param>
        protected override void OnNavigatedFrom(Windows.UI.Xaml.Navigation.NavigationEventArgs e)
        {
            sensor.StopSensorReadingOrientation();

            displayTimer.Stop();
        }

        #region Display
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Globalization", "CA1303:Literale nicht als lokalisierte Parameter übergeben", MessageId = "Windows.UI.Xaml.Controls.TextBlock.put_Text(System.String)")]
        private void Display(object sender, object e)
        {
            quaternionsDisplay.Text = "";
            lock (quaternions)
            {
                foreach (UInt32 key in quaternions.Keys)
                {
                    quaternionsDisplay.Text += key + "   " + quaternions[key].ToString();
                    quaternionsDisplay.Text += "\n";
                }
            }
            
        }
        #endregion
        #region Orientation Sensor
        /// <summary>
        /// Event when a new OrientationSensorReading is available
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="message"></param>
        private void SensorReadingOrientation(object sender, Quaternion message)
        {
            lock (quaternions)
            {
                quaternions[ORIENTATION_SENSOR_ID] = message;
            }
        }
        /// <summary>
        /// Starts the orientation sensor reading
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void sendOrientation_Checked(object sender, RoutedEventArgs e)
        {
            sensor.StartSensorReadingOrientation();
        }
        /// <summary>
        /// stops the orientation sensor reading
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void sendOrientation_Unchecked(object sender, RoutedEventArgs e)
        {
            sensor.StopSensorReadingOrientation();
        }

        #endregion
        #region BLE
        /// <summary>
        /// Initialize the GATTSevices
        /// </summary>
        private async void GetSensorTagDevice()
        {
            rFduinoDevice = (await DeviceInformation.FindAllAsync(GattDeviceService.GetDeviceSelectorFromUuid(RFduinoDeviceId), null)).FirstOrDefault();
            if (rFduinoDevice == null)
            {
                MessageDialog dialog = new MessageDialog("Service not found!");
                return;
            }

            rFduinoService = await GattDeviceService.FromIdAsync(rFduinoDevice.Id);
            if (rFduinoService == null)
            {
                MessageDialog dialog = new MessageDialog("Could not create GattDeviceService!");
                return;
            }

            readerChars = rFduinoService.GetCharacteristics(RFduinoReaderServiceId).FirstOrDefault();
            if (readerChars == null)
            {
                MessageDialog dialog = new MessageDialog("Could not get Characteristic!");
                return;
            }

            infos.Content = "";
            infos.Content += rFduinoDevice.Name + "\n";
            infos.Content += rFduinoService.Uuid + "\n";
        }

        /// <summary>
        /// Waits for messages and handles them
        /// </summary>
        /// <returns></returns>
        private async Task ReadSensorString()
        {
            if (readerChars == null)
            {
                stopReceive();
                return;
            }

            while (receiveButton.IsChecked == true)
            {
                //var readerChars = rFduinoService.GetCharacteristics(RFduinoReaderServiceId).FirstOrDefault();

                var buffer = (await readerChars.ReadValueAsync(BluetoothCacheMode.Uncached)).Value;

                if (buffer == null)
                {
                    return;
                }

                using (DataReader dataReader = DataReader.FromBuffer(buffer))
                {
                    receivedMessage.Text = "";

                    //Convert Message to string (one quaternion)
                    byte[] bytes = new byte[buffer.Length];
                    dataReader.ReadBytes(bytes);
                    string result = ParseString(bytes);
                    receivedMessage.Text = result;

                    //Parse and save the quaternion
                    ParseStringMessage(result);

                    numOfMessages++;
                    messagesPerSecond.Text = numOfMessages.ToString();

                }
            }
        }
        /// <summary>
        /// Parses a message and saves the quaternion into the dictionary
        /// assuming: values w,x,y,z in this order, 5bytes each
        /// </summary>
        /// <param name="message"></param>
        private void ParseStringMessage(string message)
        {
            if (message.Length != 20) return;
            int wIndx = 0;
            int xIndx = 5;
            int yIndx = 10;
            int zIndx = 15;

            string w = message.Substring(wIndx, 5);
            string x = message.Substring(xIndx, 5);
            string y = message.Substring(yIndx, 5);
            string z = message.Substring(zIndx, 5);

            try
            {
                float wf = GetRealFloat(float.Parse(w, System.Globalization.CultureInfo.InvariantCulture));
                float xf = GetRealFloat(float.Parse(x, System.Globalization.CultureInfo.InvariantCulture));
                float yf = GetRealFloat(float.Parse(y, System.Globalization.CultureInfo.InvariantCulture));
                float zf = GetRealFloat(float.Parse(z, System.Globalization.CultureInfo.InvariantCulture));

                if (float.IsNaN(wf) || float.IsNaN(xf) || float.IsNaN(yf) || float.IsNaN(zf)) return;
                if (!((wf >= -1 && wf <= 1) && (xf >= -1 && xf <= 1) && (yf >= -1 && yf <= 1) && (zf >= -1 && zf <= 1))) return;

                Quaternion quat = new Quaternion(wf, xf, yf, zf);
                lock (quaternions)
                {
                    quaternions[BLE_ID] = quat;
                }
            }
            catch (FormatException)
            {

            }
        }
        /// <summary>
        /// float.Parse here doesn't return the correct value
        /// Sets the comma to the right spot
        /// </summary>
        private static float GetRealFloat(float f)
        {
            if (f < 0) return f / 100;
            else return f / 1000;
        }

        /// <summary>
        /// Converts a byte array into a string
        /// </summary>
        private static string ParseString(byte[] bytes)
        {
            return Encoding.UTF8.GetString(bytes, 0, bytes.Length);
        }

        /****Events*****/

        /// <summary>
        /// If the BLE-receive-button gets unchecked, stop receiving
        /// </summary>
        private void stopReceive()
        {
            receiveButton.IsChecked = false;
        }

        /// <summary>
        /// If the BLE-receive-button gets checked, start receiving
        /// </summary>
        private async void receiveButton_Checked(object sender, RoutedEventArgs e)
        {
            //if Characteristic is not initialized, initialize
            if (readerChars == null)
            {
                GetSensorTagDevice();
            }
            //try to receive
            await ReadSensorString();
        }

        /// <summary>
        /// If the GATTButton gets clicked, initialize GattServices
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void getGATTButton_Click(object sender, RoutedEventArgs e)
        {
            stopReceive();
            GetSensorTagDevice();
        }

        #endregion
        #region Wlan

        private double oldtime = 0;
        static DateTime referenceTime = new DateTime(2010, 1, 1);
        static public long Millis()
        {
            return (long)(DateTime.UtcNow - referenceTime).TotalMilliseconds;
        }

        /// <summary>
        /// Sends whats in the dictionary every SEND_WLAN_DELAY milliseconds
        /// </summary>
        /// <returns></returns>
        private async Task SendLastQuatsWlan()
        {
            Dictionary<UInt32, Quaternion> quatsCopy = new Dictionary<UInt32, Quaternion>();
            while (wlanSendButton.IsChecked == true)
            {
                if (Millis() - oldtime > SEND_WLAN_DELAY)
                {
                    oldtime = Millis();
                    lock (quaternions)
                    {
                        //have to copy the quaternions or i'll get problems with the loop, because 'quaternions' get changed elsewhere
                        quatsCopy = new Dictionary<UInt32, Quaternion>(quaternions);
                    }
                    sendTimeText.Text = quatsCopy.Count + "  " + oldtime.ToString() + " ";
                    foreach (UInt32 key in quatsCopy.Keys)
                    {
                        await SendMessageWlan(key, quatsCopy[key]);
                        sendTimeText.Text += "|";
                    }
                }

                await Task.Delay(1);
            }
        }

        /// <summary>
        /// Sends Debugmessage over WLAN
        /// </summary>
        private async Task SendDebugWlan()
        {
            Dictionary<UInt32, Quaternion> quatsCopy = new Dictionary<UInt32, Quaternion>();
            while (sendDebugButton.IsChecked == true)
            {
                await SendMessageWlan(1000, new Quaternion(1, 0, 0, 0));
                await Task.Delay(2);
            }
        }
        /// <summary>
        /// Sends a message over Wlan
        /// </summary>
        /// <param name="timestamp">here the id of the Sensor</param>
        /// <param name="q">quaternion to send</param>
        private async Task SendMessageWlan(UInt32 timestamp, Quaternion q)
        {
            var socket = new DatagramSocket();

            using (var stream = await socket.GetOutputStreamAsync(new HostName(wlanHost), wlanPort))
            {
                using (var writer = new DataWriter(stream))
                {

                    byte[] w = BitConverter.GetBytes(q.W);
                    byte[] x = BitConverter.GetBytes(q.X);
                    byte[] y = BitConverter.GetBytes(q.Y);
                    byte[] z = BitConverter.GetBytes(q.Z);
                    byte[] time = BitConverter.GetBytes(timestamp);
                    writer.WriteBytes(time);
                    writer.WriteBytes(w);
                    writer.WriteBytes(x);
                    writer.WriteBytes(y);
                    writer.WriteBytes(z);
                    await writer.StoreAsync();
                }
            }
        }

        /******Events********/
        private void StartSendWlan(object sender, RoutedEventArgs e)
        {
            Task sendWlan = SendLastQuatsWlan();
        }

        private async void SendDebugButton(object sender, RoutedEventArgs e)
        {
            await SendDebugWlan();
        }

        private void portName_LostFocus(object sender, RoutedEventArgs e)
        {
            wlanPort = portNameBox.Text;
        }

        private void hostNameBox_LostFocus(object sender, RoutedEventArgs e)
        {
            wlanHost = hostNameBox.Text;
        }

        #endregion
    }
}
