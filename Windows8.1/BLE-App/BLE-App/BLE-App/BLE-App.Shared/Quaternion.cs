using System;

namespace BlueConnect
{
    /// <summary>
    /// Simple class for storing the floats (w, x, y, z)
    /// </summary>
    public class Quaternion : EventArgs
    {
        /// <summary>
        /// The w-component of the quaternion
        /// </summary>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "W")]
        public float W { get; set; }
        /// <summary>
        /// The x-component of the quaternion
        /// </summary>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "X")]
        public float X { get; set; }
        /// <summary>
        /// The y-component of the quaternion
        /// </summary>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "Y")]
        public float Y { get; set; }
        /// <summary>
        /// The z-component of the quaternion
        /// </summary>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "Z")]
        public float Z { get; set; }
        /// <summary>
        /// Creates a quaternion
        /// </summary>
        /// <param name="w">w component</param>
        /// <param name="x">x component</param>
        /// <param name="y">y component</param>
        /// <param name="z">z component</param>
        [System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "w"), System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "x"), System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "y"), System.Diagnostics.CodeAnalysis.SuppressMessage("Microsoft.Naming", "CA1704:IdentifiersShouldBeSpelledCorrectly", MessageId = "z")]
        public Quaternion(float w, float x, float y, float z)
        {
            W = w;
            X = x;
            Y = y;
            Z = z;
        }

        public override string ToString()
        {
            return "w:" + W + "  x:" + X + "  y:" + Y + "  z:" + Z;
        }
    }
}
