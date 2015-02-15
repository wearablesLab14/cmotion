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
using Windows.UI.Xaml.Shapes;
using Windows.Graphics.Display;
using BlueConnect;

namespace BluetoothApp
{
    public sealed partial class Scenario1 : Page
    {
        // A pointer back to the main page.  This is needed if you want to call methods in MainPage such
        // as NotifyUser()
        MainPage rootPage = MainPage.Current;

        public Scenario1()
        {
            this.InitializeComponent();

            //Event when read a new quaternion from the OrientationSensor
            App.SensorManager.SensorReadingOrientation += SensorReadingOrientation;

            if (!App.SensorManager.HasOrientationSensor())
            {
                rootPage.NotifyUser("No orientation-sensor found", NotifyType.ErrorMessage);
            }
        }

        /// <summary>
        /// Vizualize the quaternion read by the SensorManager
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="q">the quaternion</param>
        private void SensorReadingOrientation(object sender, Quaternion q)
        {
            OrientationOutput_W.Text = string.Format("{0:f4}", q.W);
            OrientationOutput_X.Text = string.Format("{0:f4}", q.X);
            OrientationOutput_Y.Text = string.Format("{0:f4}", q.Y);
            OrientationOutput_Z.Text = string.Format("{0:f4}", q.Z);
        }

        /// <summary>
        /// This is the event handler for VisibilityChanged events. You would register for these notifications
        /// if handling sensor data when the app is not visible could cause unintended actions in the app.
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e">
        /// Event data that can be examined for the current visibility state.
        /// </param>
        private void VisibilityChanged(object sender, VisibilityChangedEventArgs e)
        {
            if (enableOrientationButton.IsChecked == true)
            {
                if (e.Visible)
                {
                    // Re-enable sensor input (no need to restore the desired reportInterval... it is restored for us upon app resume)
                    App.SensorManager.StartSensorReadingOrientation();
                }
                else
                {
                    // Disable sensor input (no need to restore the default reportInterval... resources will be released upon app suspension)
                    App.SensorManager.StopSensorReadingOrientation();
                }
            }
        }

        /// <summary>
        /// Invoked when this page is about to be displayed in a Frame.
        /// </summary>
        /// <param name="e">Event data that describes how this page was reached. The Parameter
        /// property is typically used to configure the page.</param>
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
        }

        /// <summary>
        /// Invoked immediately before the Page is unloaded and is no longer the current source of a parent Frame.
        /// </summary>
        /// <param name="e">
        /// Event data that can be examined by overriding code. The event data is representative
        /// of the navigation that will unload the current Page unless canceled. The
        /// navigation can potentially be canceled by setting Cancel.
        /// </param>
        protected override void OnNavigatingFrom(NavigatingCancelEventArgs e)
        {
            if (enableOrientationButton.IsChecked == true)
            {
                Window.Current.VisibilityChanged -= new WindowVisibilityChangedEventHandler(VisibilityChanged);
                App.SensorManager.StopSensorReadingOrientation();
            }

            base.OnNavigatingFrom(e);
        }

        private void enableOrientationButton_Checked(object sender, RoutedEventArgs e)
        {
            if (!App.SensorManager.HasOrientationSensor()) return;
            Window.Current.VisibilityChanged += new WindowVisibilityChangedEventHandler(VisibilityChanged);
            App.SensorManager.StartSensorReadingOrientation();
        }

        private void enableOrientationButton_Unchecked(object sender, RoutedEventArgs e)
        {
            Window.Current.VisibilityChanged -= new WindowVisibilityChangedEventHandler(VisibilityChanged);
            App.SensorManager.StopSensorReadingOrientation();
        }
    }
}
