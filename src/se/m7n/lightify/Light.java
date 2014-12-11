/*
 * Copyright 2014 Mikael Magnusson
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package se.m7n.lightify;

import java.io.IOException;
import java.nio.ByteBuffer;

public class Light extends Luminary
{
    private Connection mConn;
    private Address mAddr;
    private boolean mOn;
    private byte mLum;
    private short mTemp;
    private byte mR;
    private byte mG;
    private byte mB;

    public static class Address
    {
        private byte[] mAddr;

        public Address(byte[] addr)
        {
            mAddr = addr;
        }

        public byte[] data()
        {
            return mAddr;
        }

        public String toString()
        {
            return Connection.toHexString(mAddr);
        }
    }

    public Light(Connection conn, Address addr, String name)
    {
        super(conn, name);
        mConn = conn;
        mAddr = addr;
    }

    public Address addr()
    {
        return mAddr;
    }

    public void updateStatus(boolean on, byte lum, short temp, byte r, byte g, byte b)
    {
        mOn = on;
        mLum = lum;
        mTemp = temp;
        mR = r;
        mG = g;
        mB = b;
    }

    public boolean on()
    {
        return mOn;
    }

    public void setOnOff(boolean on)
        throws IOException
    {
        mOn = on;
        super.setOnOff(on);
    }

    public byte lum()
    {
        return mLum;
    }

    public void setLuminance(byte lum, short time)
        throws IOException
    {
        mLum = lum;
        super.setLuminance(lum, time);
    }

    public short temp()
    {
        return mTemp;
    }

    public void setTemperature(short temp, short time)
        throws IOException
    {
        mTemp = temp;
        super.setTemperature(temp, time);
    }

    public byte red() { return mR; }
    public byte green() { return mG; }
    public byte blue() { return mB; }
            
    public void setRgb(byte r, byte g, byte b, short time)
        throws IOException
    {
        mR = r;
        mG = g;
        mB = b;
        super.setRgb(r, g, b, time);
    }

    public ByteBuffer buildCommand(byte command, byte[] data)
    {
        return mConn.buildLightCommand(command, this, data);
    }

    public String toString()
    {
        return "<light: " + name() + ">";
    }
}
