using System;
using System.Collections.Generic;
using System.Text;

namespace BlueConnect
{
    public class Quaternion : EventArgs
    {
        public float W { get; set; }
        public float X { get; set; }
        public float Y { get; set; }
        public float Z { get; set; }
        public Quaternion(float w, float x, float y, float z)
        {
            W = w;
            X = x;
            Y = y;
            Z = z;
        }

        public override string ToString()
        {
            return "W: " + MyFormat(W) + "   X: " + MyFormat(X) + "   Y: " + MyFormat(Y) + "   Z: " + MyFormat(Z);
        }

        private static string MyFormat(float x)
        {
            return string.Format("{0:f5}", x);
        }
    }
}
