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

namespace BluetoothApp
{
    public sealed partial class Scenario2 : Page
    {
        // A pointer back to the main page.  This is needed if you want to call methods in MainPage such
        // as NotifyUser()
        MainPage rootPage = MainPage.Current;

        public Scenario2()
        {
            this.InitializeComponent();
        }

        /// <summary>
        /// Invoked when this page is about to be displayed in a Frame.
        /// </summary>
        /// <param name="e">Event data that describes how this page was reached. The Parameter
        /// property is typically used to configure the page.</param>
        protected override void OnNavigatedTo(NavigationEventArgs e)
        {
            hostName.Text = App.WlanHostName;
            portName.Text = App.WlanPort;
        }

        /// <summary>
        /// When the Textbox of the hostname loses it's focus
        /// the user finished to enter the hostname -> save it
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void hostName_LostFocus(object sender, RoutedEventArgs e)
        {
            App.WlanHostName = hostName.Text;
        }

        /// <summary>
        /// When the Textbox of the port loses it's focus
        /// the user finished to enter it -> save it
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void portName_LostFocus(object sender, RoutedEventArgs e)
        {
            App.WlanPort = portName.Text;
        }

        /// <summary>
        /// Invoked immediately before the Page is unloaded and is no longer the current source of a parent Frame.
        /// </summary>
        /// <param name="e">
        /// Event data that can be examined by overriding code. The event data is representative
        /// of the navigation that will unload the current Page unless canceled. The
        /// navigation can potentially be canceled by setting Cancel.
        /// </param>
        /*protected override void OnNavigatingFrom(NavigatingCancelEventArgs e)
        {

            base.OnNavigatingFrom(e);
        }*/
        
    }
}
