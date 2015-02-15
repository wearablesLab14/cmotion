using System;

namespace RfduinoBleApp
{
    /// <summary>
    /// Exception if initialization of main page fails
    /// </summary>
    public class InitException : Exception
    {
        public InitException(String msg) : base(msg) { }

        public InitException(String msg, Exception innerException) : base(msg, innerException) { }

        public InitException() : base() { }
    }
}
