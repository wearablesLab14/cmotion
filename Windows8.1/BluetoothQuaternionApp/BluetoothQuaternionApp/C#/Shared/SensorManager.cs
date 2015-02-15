using BlueConnect;
using System;
using System.Collections.Generic;
using System.Text;
using Windows.Devices.Sensors;
using Windows.UI.Xaml;

namespace Sensors
{
    public class SensorManager
    {
        private OrientationSensor orientationSensor;
        public uint DesiredReportIntervalOrientation {get; private set;}
        private DispatcherTimer dispatcherTimerOrientation;

        internal delegate void OrientationSensorReadingDelegate(object sender, Quaternion message);
        internal event OrientationSensorReadingDelegate SensorReadingOrientation;

        public SensorManager()
        {
            orientationSensor = OrientationSensor.GetDefault();
            if (orientationSensor != null)
            {
                uint minReportInterval = orientationSensor.MinimumReportInterval;
                DesiredReportIntervalOrientation = minReportInterval > 16 ? minReportInterval : 16;

                dispatcherTimerOrientation = new DispatcherTimer();
                dispatcherTimerOrientation.Tick += OrientationReading;
                dispatcherTimerOrientation.Interval = new TimeSpan(0, 0, 0, 0, (int)DesiredReportIntervalOrientation);
            }
        }

        /// <summary>
        /// Start reading the Orientationsensor
        /// </summary>
        public void StartSensorReadingOrientation()
        {
            if (orientationSensor == null) return;
            orientationSensor.ReportInterval = DesiredReportIntervalOrientation;
            dispatcherTimerOrientation.Start();
        }

        /// <summary>
        /// Stop reading the Orientationsensor
        /// </summary>
        public void StopSensorReadingOrientation()
        {
            if (orientationSensor == null) return;
            dispatcherTimerOrientation.Stop();
            orientationSensor.ReportInterval = 0;
        }

        /// <summary>
        /// New OrientationSensorReading
        /// </summary>
        /// <param name="sender"></param>
        /// <param name="e"></param>
        private void OrientationReading(object sender, object e)
        {
            SensorReadingOrientation(sender, getOrientationSensorReading());
        }

        private Quaternion getOrientationSensorReading(){
            if (orientationSensor == null) return new Quaternion(1, 0, 0, 0);
            OrientationSensorReading reading = orientationSensor.GetCurrentReading();

            if (reading == null) return new Quaternion(1, 0, 0, 0);
            return new Quaternion(reading.Quaternion.W,
                reading.Quaternion.X,
                reading.Quaternion.Y,
                reading.Quaternion.Z);
        }

        public bool HasOrientationSensor()
        {
            return orientationSensor != null;
        }
    }
}
