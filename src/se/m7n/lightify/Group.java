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

import java.nio.ByteBuffer;

public class Group extends Luminary
{
    private Connection mConn;
    private byte mIdx;
    private Light.Address[] mLights;

    public Group(Connection conn, byte idx, String name)
    {
        super(conn, name);
        mConn = conn;
        mIdx = idx;
        mLights = new Light.Address[0];
    }

    public byte idx()
    {
        return mIdx;
    }

    public Light.Address[] lights()
    {
        return mLights;
    }

    public void setLights(Light.Address[] lights)
    {
        mLights = lights;
    }

    public ByteBuffer buildCommand(byte command, byte[] data)
    {
        return mConn.buildCommand(command, this, data);
    }

    // TODO
    // public String toString()
    // {
    //     StringBuffer s;
    //     for light_addr in self.lights():
    //         if light_addr in self.__conn.lights():
    //             light = self.__conn.lights()[light_addr]
    //         else:
    //             light = "%x" % light_addr
    //         s = s + str(light) + " "

    //     return "<group: %s, lights: %s>" % (self.name(), s)

    //     return "<light: " + name() + ">";
    // }
}
