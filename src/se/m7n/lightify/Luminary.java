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

abstract public class Luminary
{
    private String mName;
    private Connection mConn;

    public Luminary(Connection conn, String name)
    {
        mConn = conn;
        mName = name;
    }

    public String name() {
        return mName;
    }

    public void setOnOff(boolean on)
        throws IOException
    {
        ByteBuffer data = mConn.buildOnOff(this, on);
        mConn.send(data);
        mConn.recv();
    }

    public void setLuminance(byte lum, short time)
        throws IOException
    {
        ByteBuffer data = mConn.buildLuminance(this, lum, time);
        mConn.send(data);
        mConn.recv();
    }

    public void setTemperature(short temp, short time)
        throws IOException
    {
        ByteBuffer data = mConn.buildTemp(this, temp, time);
        mConn.send(data);
        mConn.recv();
    }

    public void setRgb(byte r, byte g, byte b, short time)
        throws IOException
    {
        ByteBuffer data = mConn.buildColor(this, r, g, b, time);
        mConn.send(data);
        mConn.recv();
    }

    abstract public ByteBuffer buildCommand(byte command, byte[] data);
}
