
//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Navigation;
using SDKTemplate;
using System;
using Windows.Devices.Sensors;
using Windows.Foundation;
using System.Threading.Tasks;
using Windows.UI.Core;
using System.Collections.Generic;
using Windows.UI.Popups;
using BlueConnect;
using System.Threading;
using Windows.Networking.Sockets;
using Windows.Networking;
using Windows.Storage.Streams;
using System.Runtime.InteropServices.WindowsRuntime;

namespace BluetoothApp
{
    public sealed partial class Scenario3 : Page
    {
        private const int MAX_CONSOLE_CHARS = 2500;

        //how many millisec should be waited between wlan-activity
        private const int SEND_WLAN_DELAY = 5;

        //Handling Id's of Sensors
        private const UInt32 ORIENTATION_SENSOR_ID = 10; //Id for the devices Sensor
        private const int MAX_SENSORS_PER_DEVICE = 3; //Offset for getting distinct id's for all Sensors

        /// <summary>
        /// Dictionary to save quaternions: key = Id of sensor, value = last quaternion
        /// </summary>
        private Dictionary<UInt32, Quaternion> quaternions = new Dictionary<UInt32, Quaternion>();

        //Display
        private const int DISPLAY_INTERVAL = 30; //time between updates of the display-values
        private DispatcherTimer displayTimer;

        public Scenario3()
        {
            this.InitializeComponent();
            //Events to capture Bluetooth-activity (for two different devices)
            App.BluetoothManager1.MessageReceived += BluetoothManager1_MessageReceived;
            App.BluetoothManager1.ExceptionOccured += BluetoothManager1_ExceptionOccured;
            App.BluetoothManager2.MessageReceived += BluetoothManager2_MessageReceived;
            App.BluetoothManager2.ExceptionOccured += BluetoothManager2_ExceptionOccured;

            oldtime = Millis();

            //Own sensors
            App.SensorManager.SensorReadingOrientation += SensorReadingOrientation;

            //initialize Dispatcher to display quaternions
            displayTimer = new DispatcherTimer();
            displayTimer.Tick += Display;
            displayTimer.Interval = new TimeSpan(0, 0, 0, 0, DISPLAY_INTERVAL);
            displayTimer.Start();
        }

        protected override void OnNavigatedFrom(Windows.UI.Xaml.Navigation.NavigationEventArgs e)
        {
            //clean up the mess
            App.BluetoothManager1.Disconnect();
            App.BluetoothManager2.Disconnect();

            App.SensorManager.StopSensorReadingOrientation();

            displayTimer.Stop();
        }

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

        private void sendOrientation_Checked(object sender, RoutedEventArgs e)
        {
            App.SensorManager.StartSensorReadingOrientation();
        }

        private void sendOrientation_Unchecked(object sender, RoutedEventArgs e)
        {
            App.SensorManager.StopSensorReadingOrientation();
        }

        #endregion
        #region Display

        /// <summary>
        /// Display all quaternions in the Dictionary
        /// </summary>
        private void Display(object sender, object e)
        {
            ClearLines();
            Dictionary<UInt32, Quaternion> tmp = new Dictionary<UInt32, Quaternion>();
            lock (quaternions)
            {
                tmp = new Dictionary<uint,Quaternion>(quaternions);
            }
            foreach (UInt32 key in tmp.Keys)
            {
                WriteLine(key + "   " + tmp[key].ToString());
            }
        }

        /// <summary>
        /// write a new line to the "Console"
        /// </summary>
        /// <param name="line">content</param>
        private void WriteLine(string line)
        {
            console.Text += line + "\n";
            scrollViewer.ChangeView(0, scrollViewer.ScrollableHeight, 1, false);
            //Empty the Console if it geht too full
            if (console.Text.Length > MAX_CONSOLE_CHARS)
            {
                ClearLines();
            }
        }
        private void ClearLines()
        {
            console.Text = "";
        }

        #endregion
        #region Bluetooth Connection Lifecycle
        private async void BluetoothConnect1_Click(object sender, RoutedEventArgs e)
        {
            quaternions.Clear(); //reset quaternion cache
            //ask the user to connect
            Rect r = new Rect(0, 600, 200, 100);
            await App.BluetoothManager1.EnumerateDevicesAsync(r);
        }
        private async void BluetoothConnect2_Click(object sender, RoutedEventArgs e)
        {
            quaternions.Clear(); //reset quaternion cache
            //ask the user to connect
            Rect r = new Rect(0, 600, 200, 100);
            await App.BluetoothManager2.EnumerateDevicesAsync(r);
        }
        private async void BluetoothManager1_ExceptionOccured(object sender, Exception ex)
        {
            var md = new MessageDialog(ex.Message, "We've got a problem with bluetooth...");
            md.Commands.Add(new UICommand("Ah.. thanks.."));
            md.DefaultCommandIndex = 0;
            var result = await md.ShowAsync();
        }
        private async void BluetoothManager2_ExceptionOccured(object sender, Exception ex)
        {
            var md = new MessageDialog(ex.Message, "We've got a problem with bluetooth...");
            md.Commands.Add(new UICommand("Ah.. thanks.."));
            md.DefaultCommandIndex = 0;
            var result = await md.ShowAsync();
        }
        #endregion
        #region Send & Receive
        /// <summary>
        /// Sending an x to the Bluetoothdevice
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private async void SendXButton1_Click(object sender, RoutedEventArgs e)
        {
            string cmd = "x";
            //try to send this message
            var res = await App.BluetoothManager1.SendMessageAsync(cmd);
            if (res == 1)//log if successful
                WriteLine("> " + cmd);
        }

        private async void SendXButton2_Click(object sender, RoutedEventArgs e)
        {
            string cmd = "x";
            //try to send this message
            var res = await App.BluetoothManager2.SendMessageAsync(cmd);
            if (res == 1)//log if successful
                WriteLine("> " + cmd);
        }

        /// <summary>
        /// Parses the message and saves the quaternion
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="message"></param>
        private void BluetoothManager1_MessageReceived(object sender, string message)
        {
            if (message[0] != 'q') return;
            int qIndx = 0;
            int wIndx = message.IndexOf('w');
            int xIndx = message.IndexOf('x');
            int yIndx = message.IndexOf('y');
            int zIndx = message.IndexOf('z');

            if (wIndx == -1 || xIndx == -1 || yIndx == -1 || zIndx == -1) return;

            string q = message.Substring(qIndx + 1, wIndx - qIndx - 1); //substring(index,length)
            string w = message.Substring(wIndx + 1, xIndx - wIndx - 1);
            string x = message.Substring(xIndx + 1, yIndx - xIndx - 1);
            string y = message.Substring(yIndx + 1, zIndx - yIndx - 1);
            string z = message.Substring(zIndx + 1, message.Length - zIndx - 2);

            UInt32 q_int = UInt32.Parse(q);
            float wf = float.Parse(w);
            float xf = float.Parse(x);
            float yf = float.Parse(y);
            float zf = float.Parse(z);

            if (float.IsNaN(wf) || float.IsNaN(xf) || float.IsNaN(yf) || float.IsNaN(zf)) return;
            if (!((wf >= -1 && wf <= 1) && (xf >= -1 && xf <= 1) && (yf >= -1 && yf <= 1) && (zf >= -1 && zf <= 1))) return;

            Quaternion quat = new Quaternion(wf, xf, yf, zf);
            lock (quaternions)
            {
                quaternions[q_int] = quat;
            }
        }

        private void BluetoothManager2_MessageReceived(object sender, string message)
        {
            if (message[0] != 'q') return;
            int qIndx = 0;
            int wIndx = message.IndexOf('w');
            int xIndx = message.IndexOf('x');
            int yIndx = message.IndexOf('y');
            int zIndx = message.IndexOf('z');

            if (wIndx == -1 || xIndx == -1 || yIndx == -1 || zIndx == -1) return;

            string q = message.Substring(qIndx + 1, wIndx - qIndx - 1); //substring(index,length)
            string w = message.Substring(wIndx + 1, xIndx - wIndx - 1);
            string x = message.Substring(xIndx + 1, yIndx - xIndx - 1);
            string y = message.Substring(yIndx + 1, zIndx - yIndx - 1);
            string z = message.Substring(zIndx + 1, message.Length - zIndx - 2);

            UInt32 q_int = UInt32.Parse(q) + MAX_SENSORS_PER_DEVICE;
            float wf = float.Parse(w);
            float xf = float.Parse(x);
            float yf = float.Parse(y);
            float zf = float.Parse(z);

            if (float.IsNaN(wf) || float.IsNaN(xf) || float.IsNaN(yf) || float.IsNaN(zf)) return;
            if (!((wf >= -1 && wf <= 1) && (xf >= -1 && xf <= 1) && (yf >= -1 && yf <= 1) && (zf >= -1 && zf <= 1))) return;

            Quaternion quat = new Quaternion(wf,xf,yf,zf);
            lock (quaternions)
            {
                quaternions[q_int] = quat;
            }
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
        private void StartSendWlan(object sender, RoutedEventArgs e)
        {
            Task sendWlan = SendLastQuatsWlan();
        }

        /// <summary>
        /// Sends a message over Wlan
        /// </summary>
        /// <param name="timestamp">here the id of the Sensor</param>
        /// <param name="q">quaternion to send</param>
        private async Task SendMessageWlan(UInt32 timestamp, Quaternion q)
        {
            var socket = new DatagramSocket();

            using (var stream = await socket.GetOutputStreamAsync(new HostName(App.WlanHostName), App.WlanPort))
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

        private void SendDebugButton(object sender, RoutedEventArgs e)
        {
            SendDebugWlan();
        }

        private async Task SendDebugWlan()
        {
            Dictionary<UInt32, Quaternion> quatsCopy = new Dictionary<UInt32, Quaternion>();
            while (sendDebugButton.IsChecked == true)
            {
                await SendMessageWlan(1000, new Quaternion(1, 0, 0, 0));
                await Task.Delay(2);
            }
        }

        #endregion
        #region helper
        /// <summary>
        /// combines two byte arrays into one
        /// </summary>
        private static byte[] Combine(byte[] first, byte[] second)
        {
            byte[] result = new byte[first.Length + second.Length];
            System.Buffer.BlockCopy(first, 0, result, 0, first.Length);
            System.Buffer.BlockCopy(second, 0, result, first.Length, second.Length);
            return result;
        }
        #endregion
    }
}