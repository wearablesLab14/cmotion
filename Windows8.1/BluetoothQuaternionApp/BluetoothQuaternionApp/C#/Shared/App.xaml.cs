// Copyright (c) Microsoft. All rights reserved.

using System;
using System.Windows;
using System.Collections.Generic;
using System.IO;
using System.Linq;
using System.Runtime.InteropServices.WindowsRuntime;
using Windows.ApplicationModel;
using Windows.ApplicationModel.Activation;
using Windows.Foundation;
using Windows.Foundation.Collections;
using Windows.UI.Xaml;
using Windows.UI.Xaml.Controls;
using Windows.UI.Xaml.Controls.Primitives;
using Windows.UI.Xaml.Data;
using Windows.UI.Xaml.Input;
using Windows.UI.Xaml.Media;
using Windows.UI.Xaml.Media.Animation;
using Windows.UI.Xaml.Navigation;
using SDKTemplate.Common;
using TCD.Arduino.Bluetooth;
using Sensors;
using Windows.System.Display;

/*******Sources*********
Bluetooth:
Bluetooth communication between Arduino and Windows 8.1 (https://code.msdn.microsoft.com/Bluetooth-communication-7130c260)
Wlan:
http://metronuggets.com/2013/03/18/how-to-send-and-receive-a-udp-broadcast-in-windows-phone-8-and-win8/
Sensor / App-template:
https://code.msdn.microsoft.com/universal-windows-app-cb3248c3 (OrientationSensor Sample, Accelerometer sensor sample (As App-template))
 */



namespace SDKTemplate
{
    /// <summary>
    /// Provides application-specific behavior to supplement the default Application class.
    /// </summary>
    sealed partial class App : Application
    {
        //static properties for easy access of the XAML-defined BluetoothManager resource
        public static BluetoothConnectionManager BluetoothManager1 { get { return App.Current.Resources["BluetoothManager1"] as BluetoothConnectionManager; } }
        public static BluetoothConnectionManager BluetoothManager2 { get { return App.Current.Resources["BluetoothManager2"] as BluetoothConnectionManager; } }

        //the wlan address & port to send quaternions to
        private static string wlanHostName = "192.168.0.255";
        public static string WlanHostName
        {
            get { return App.wlanHostName; }
            set { App.wlanHostName = value; }
        }
        private static string wlanPort = "5050";
        public static string WlanPort
        {
            get { return App.wlanPort; }
            set { App.wlanPort = value; }
        }

        //For easy access to the devices Orientation Sensor
        public static SensorManager SensorManager {get; private set;}

        //Needed to forbid the App to lock the screen
        private DisplayRequest lockScreen;

        /// <summary>
        /// Initializes the singleton Application object.  This is the first line of authored code
        /// executed, and as such is the logical equivalent of main() or WinMain().
        /// </summary>
        public App()
        {
            this.InitializeComponent();
            this.Suspending += OnSuspending;
            SensorManager = new SensorManager();

            lockScreen = new Windows.System.Display.DisplayRequest();
        }

        /// <summary>
        /// Invoked when the application is launched normally by the end user.  Other entry points
        /// will be used such as when the application is launched to open a specific file.
        /// </summary>
        /// <param name="e">Details about the launch request and process.</param>
        protected override async void OnLaunched(LaunchActivatedEventArgs e)
        {
            if (lockScreen != null)
            {
                lockScreen.RequestActive();
            }

            Frame rootFrame = Window.Current.Content as Frame;

            // Do not repeat app initialization when the Window already has content,
            // just ensure that the window is active

            if (rootFrame == null)
            {
                // Create a Frame to act as the navigation context and navigate to the first page
                rootFrame = new Frame();

                // Set the default language
                rootFrame.Language = Windows.Globalization.ApplicationLanguages.Languages[0];
                
                if (e.PreviousExecutionState == ApplicationExecutionState.Terminated)
                {
                    // Restore the saved session state only when appropriate
                    try
                    {
                        await SuspensionManager.RestoreAsync();
                    }
                    catch (SuspensionManagerException)
                    {
                        //Something went wrong restoring state.
                        //Assume there is no state and continue
                    }
                }

                // Place the frame in the current Window
                Window.Current.Content = rootFrame;
            }

            if (rootFrame.Content == null)
            {
                // When the navigation stack isn't restored navigate to the first page,
                // configuring the new page by passing required information as a navigation
                // parameter
                if (!rootFrame.Navigate(typeof(MainPage), e.Arguments))
                {
                    throw new Exception("Failed to create initial page");
                }
            }

            // Ensure the current window is active
            Window.Current.Activate();
        }

        /// <summary>
        /// Invoked when application execution is being suspended.  Application state is saved
        /// without knowing whether the application will be terminated or resumed with the contents
        /// of memory still intact.
        /// </summary>
        /// <param name="sender">The source of the suspend request.</param>
        /// <param name="e">Details about the suspend request.</param>
        private async void OnSuspending(object sender, SuspendingEventArgs e)
        {
            if (lockScreen != null)
            {
                lockScreen.RequestRelease();
            }

            var deferral = e.SuspendingOperation.GetDeferral();
            await SuspensionManager.SaveAsync();
            deferral.Complete();
        }
    }
}
