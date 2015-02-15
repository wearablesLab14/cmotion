//*********************************************************
//
// Copyright (c) Microsoft. All rights reserved.
// THIS CODE IS PROVIDED *AS IS* WITHOUT WARRANTY OF
// ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING ANY
// IMPLIED WARRANTIES OF FITNESS FOR A PARTICULAR
// PURPOSE, MERCHANTABILITY, OR NON-INFRINGEMENT.
//
//*********************************************************

using System.Collections.Generic;
using Windows.UI.Xaml.Controls;
using System;

namespace SDKTemplate
{
    public partial class MainPage : Page
    {
        public const string FEATURE_NAME = "Quaterniometer";

        List<Scenario> scenarios = new List<Scenario>
        {
            new Scenario() { Title = "Read OrientationSensor", ClassType = typeof(BluetoothApp.Scenario1) },
            new Scenario() { Title = "Wlan Settings", ClassType = typeof(BluetoothApp.Scenario2) },
            new Scenario() { Title = "Receive & Send", ClassType = typeof(BluetoothApp.Scenario3) }
        };
    }

    public class Scenario
    {
        public string Title { get; set; }

        public Type ClassType { get; set; }

        public override string ToString()
        {
            return Title;
        }
    }
}
