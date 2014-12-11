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

/*
 * WIP Java lib for Osram lightify
 * Communicates with a gateway connected to the same LAN via TCP port 4000
 * using a binary protocol
 */

package se.m7n.lightify;

import java.io.InputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.InetAddress;
import java.net.Socket;
import java.nio.CharBuffer;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.HashMap;
import java.util.logging.Logger;

public class Connection
{
    public final int PORT = 4000;

    public final byte COMMAND_ALL_LIGHT_STATUS = 0x13;
    public final byte COMMAND_GROUP_LIST = 0x1e;
    public final byte COMMAND_GROUP_INFO = 0x26;
    public final byte COMMAND_LUMINANCE = 0x31;
    public final byte COMMAND_ONOFF = 0x32;
    public final byte COMMAND_TEMP = 0x33;
    public final byte COMMAND_COLOUR = 0x36;
    public final byte COMMAND_LIGHT_STATUS = 0x68;

    private Logger mLogger;
    private Charset mCharset;
    private Socket mSock;
    private OutputStream mOs;
    private InputStream mIs;
    private int mSeq;
    private HashMap<String,Group> mGroups;
    private HashMap<Light.Address,Light> mLights;

    /*
     * Commands
     * 13 all light status (returns list of light address, light status, light name)
     * 1e group list (returns list of group id, and group name)
     * 26 group status (returns group id, group name, and list of light addresses)
     * 31 set group luminance
     * 32 set group onoff
     * 33 set group temp
     * 36 set group colour
     * 68 light status (returns light address and light status (?))
     */

    public Connection(InetAddress host) throws IOException
    {
        mLogger = Logger.getLogger(Logger.GLOBAL_LOGGER_NAME);
        mLogger.info("Lightify connection");
        mCharset = Charset.forName("ASCII");
        mSeq = 1;
        mGroups = new HashMap<String,Group>();
        mLights = new HashMap<Light.Address,Light>();

        mSock = new Socket(host, PORT);
        mIs = mSock.getInputStream();
        mOs = mSock.getOutputStream();
    }

    private ByteBuffer byteBufferAllocate(int size)
    {
        ByteBuffer buf = ByteBuffer.allocate(size);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }

    private ByteBuffer byteBufferWrap(byte[] data)
    {
        return byteBufferWrap(data, 0, data.length);
    }

    private ByteBuffer byteBufferWrap(byte[] data, int pos, int len)
    {
        ByteBuffer buf = ByteBuffer.wrap(data, pos, len);
        buf.order(ByteOrder.LITTLE_ENDIAN);
        return buf;
    }

    private Charset getCharset()
    {
        return mCharset;
    }

    /** Dict from group name to Group object. **/
    public HashMap<String,Group> groups()
    {
        return mGroups;
    }

    /** Dict from light addr to Light object. **/
    public HashMap<Light.Address,Light> lights()
    {
        return mLights;
    }

    public Light lightByName(String name)
    {
        mLogger.info("" + lights().size());

        Iterator<Light> iter = lights().values().iterator();

        while (iter.hasNext()) {
            Light light = iter.next();

            // try {
            //     mLogger.info("Light '" + toHexString(light.name().getBytes("ASCII")) + "' '" + toHexString(name.getBytes("ASCII")) + "': " + light.name().equals(name));
            // } catch (UnsupportedEncodingException e) {
            // }

            if (light.name().equals(name)) {
                return light;
            }
        }

        return null;
    }

    public int nextSeq()
    {
        mSeq = mSeq + 1;
        return mSeq;
    }

    public ByteBuffer buildGlobalCommand(byte command, byte[] data)
    {
        int length = 6 + (data != null ? data.length : 0);
        ByteBuffer buf = byteBufferAllocate(length+2);

        mLogger.info("buildGlobalCommand length:" + length);
        buf.putShort((short)length);
        buf.put((byte)0x02);
        buf.put(command);
        buf.putInt(nextSeq());
        if (data != null) {
            buf.put(data);
        }

        return buf;
    }

    public ByteBuffer buildBasicCommand(byte flag, byte command, byte[] group_or_light, byte[] data)
    {
        short length = (short)(14 + (data != null ? data.length : 0));
        ByteBuffer buf = byteBufferAllocate(length + 2);

        buf.putShort(length);
        buf.put(flag);
        buf.put(command);
        buf.putInt(nextSeq());
        buf.put(group_or_light);
        if (data != null) {
            buf.put(data);
        }

        return buf;
    }

    public ByteBuffer buildCommand(byte command, Group group, byte[] data)
    {
        int length = 14 + (data != null ? data.length : 0);
        byte[] groupIdx = new byte[8];
        groupIdx[0] = group.idx();

        return buildBasicCommand((byte)0x02, command, groupIdx, data);
    }

    public ByteBuffer buildLightCommand(byte command, Light light, byte[] data)
    {
        int length = 6 + 8 + data.length;

        return buildBasicCommand((byte)0x00, command, light.addr().data(), data);
    }

    public ByteBuffer buildOnOff(Luminary item, boolean on)
    {
        byte[] data = new byte[1];
        data[0] = (byte)(on ? 1 : 0);
        return item.buildCommand(COMMAND_ONOFF, data);
    }

    public ByteBuffer buildTemp(Luminary item, short temp, short time)
    {
        ByteBuffer buf = byteBufferAllocate(4);
        buf.putShort(temp);
        buf.putShort(time);

        return item.buildCommand(COMMAND_TEMP, buf.array());
    }

    public ByteBuffer buildLuminance(Luminary item, byte luminance, short time)
    {
        ByteBuffer buf = byteBufferAllocate(3);
        buf.put(luminance);
        buf.putShort(time);
        return item.buildCommand(COMMAND_LUMINANCE, buf.array());
    }

    public ByteBuffer buildColor(Luminary item, byte red, byte green, byte blue, short time)
    {
        ByteBuffer buf = byteBufferAllocate(6);
        buf.put(red);
        buf.put(green);
        buf.put(blue);
        buf.put((byte)0xff);
        buf.putShort(time);

        return item.buildCommand(COMMAND_COLOUR, buf.array());
    }

    public ByteBuffer buildGroupInfo(Group group)
    {
        return buildCommand(COMMAND_GROUP_INFO, group, null);
    }

    public ByteBuffer buildAllLightStatus(byte flag)
    {
        byte[] data = new byte[1];
        data[0] = flag;
        return buildGlobalCommand(COMMAND_ALL_LIGHT_STATUS, data);
    }

    public ByteBuffer buildLightStatus(Light light)
    {
        return light.buildCommand(COMMAND_LIGHT_STATUS, null);
    }

    public ByteBuffer buildGroupList()
    {
        return buildGlobalCommand(COMMAND_GROUP_LIST, null);
    }

    public HashMap<Integer,String> groupList() throws IOException
    {
        ByteBuffer data = buildGroupList();
        send(data);
        data = recv();
        data.order(ByteOrder.LITTLE_ENDIAN);
        int num = data.getShort(7);
        HashMap<Integer,String> groups = new HashMap<Integer,String>(num);
        mLogger.info("Num " + num);

        for(int i=0; i<num; i++) {
            int pos = 11+i*18;
            ByteBuffer payload = byteBufferWrap(data.array(), pos, 18);

            // Format: <H16s
            int idx = payload.getShort();
            CharBuffer nameBuf = getCharset().decode(payload);
            String name = nameBuf.toString().trim();

            groups.put(idx, name);
            mLogger.info("Idx " + idx + ": '" + name + "'");
        }

        return groups;
    }

    public void updateGroupList() throws IOException
    {
        HashMap<Integer,String> lst = groupList();
        HashMap<String,Group> groups = new HashMap<String,Group>(lst.size());

        Iterator<Integer> iter = lst.keySet().iterator();
        while(iter.hasNext()) {
            Integer idx = iter.next();
            String name = lst.get(idx);

            Group group = new Group(this, idx.byteValue(), name);
            group.setLights(groupInfo(group));
            groups.put(name, group);
        }

        mGroups = groups;
    }

    public Light.Address[] groupInfo(Group group) throws IOException
    {
        ByteBuffer data = buildGroupInfo(group);
        send(data);
        data = recv();
        ByteBuffer payload = byteBufferWrap(data.array(), 7, data.limit()-7);

        // Format: <H16s
        int idx = payload.getShort();
        CharBuffer nameBuf = getCharset().decode(byteBufferWrap(payload.array(), 2, 16));
        payload.position(payload.position() + 16);
        int num = payload.get();
        Light.Address[] lights = new Light.Address[num];
        //name = name.replace('\0', "");
        //self.__logger.debug("Idx %d: '%s' %d", idx, name, num);
        for(int i=0; i<num; i++) {
            int pos = 7 + 19 + i * 8;
            ByteBuffer lightPayload = byteBufferWrap(data.array(), pos, 8);
            // Format <Q
            byte[] addr = lightPayload.array();
            //self.__logger.debug("%d: %x", i, addr);

            lights[i] = new Light.Address(addr);

            //self.read_light_status(addr);
        }
        return lights;
    }

    static public String toHexString(byte[] bytes)
    {
        return toHexString(ByteBuffer.wrap(bytes));
    }

    static public String toHexString(ByteBuffer bytes)
    {
        ByteBuffer data = bytes.duplicate();
        data.rewind();
        StringBuffer buf = new StringBuffer();
        for(int i=0;i<data.limit();i++) {
            int value = data.get();
            String hex = Integer.toHexString(value);
            if (hex.length() <= 2) {
                if (hex.length() == 1)
                    buf.append("0");
                buf.append(hex);
            } else {
                buf.append(hex.substring(hex.length() - 2));
            }

            buf.append(" ");
        }

        return buf.toString();
    }

    public void send(ByteBuffer data) throws IOException
    {
        mLogger.info("sending " + toHexString(data));

        mOs.write(data.array());
    }

    public ByteBuffer recv() throws IOException
    {
        mLogger.info("receiving");

        int lengthsize = 2;
        byte[] data = new byte[lengthsize];
        mIs.read(data);

        ByteBuffer lenBuf = byteBufferWrap(data);

        // Format: <H
        int length = lenBuf.getShort();

        mLogger.info("len: " + length);

        ByteBuffer buf = byteBufferAllocate(lengthsize + length);
        buf.put(data);

        //self.__logger.debug(len(data))
        //string = ""
        int expected = length;
        data = new byte[1024];
        //self.__logger.debug("Length %d", length)
        //self.__logger.debug("Expected %d", expected)

        while(expected > 0) {
            //self.__logger.debug('received "%d %s"', length, binascii.hexlify(data));

            int len = expected > 1024 ? 1024 : expected;
            int res = mIs.read(data, 0, expected);

            if (res == -1) {
                // FIXME
            } else {
                expected = expected - res;
                buf.put(data, 0, res);
            }
        }
        mLogger.info("received '" + toHexString(buf) + "'");
        return buf;
    }

    public void updateLightStatus(Light light) throws IOException
    {
        ByteBuffer data = buildLightStatus(light);
        send(data);
        data = recv();
        return;


        // (on,lum,temp,red,green,blue,h) = struct.unpack("<27x2BH4B16x", data)
        // self.__logger.debug('status: %0x %0x %d %0x %0x %0x %0x', on,lum,temp,red,green,blue,h)

        // self.__logger.debug('onoff: %d', on)
        // self.__logger.debug('temp:  %d', temp)
        // self.__logger.debug('lum:   %d', lum)
        // self.__logger.debug('red:   %d', red)
        // self.__logger.debug('green: %d', green)
        // self.__logger.debug('blue:  %d', blue)
        // return (on, lum, temp, red, green, blue)
    }

    public void updateAllLightStatus() throws IOException
    {
        ByteBuffer data = buildAllLightStatus((byte)1);
        send(data);
        data = recv();
        // Format: <H
        int num = data.getShort(9);

        mLogger.info("light status num: " + num);

        HashMap<Light.Address,Light> old_lights = lights();
        HashMap<Light.Address,Light> new_lights = new HashMap<Light.Address,Light>(num);

        for(int i=0; i<num; i++) {
            int pos = 11 + i * 42;
            ByteBuffer payload = byteBufferWrap(data.array(), pos, 42);

            mLogger.info(i + " " + pos + " " + payload.limit());
            // Format: <HQ16s16s
            int a = payload.getShort();
            byte[] rawAddr = new byte[8];
            payload.get(rawAddr);
            Light.Address addr = new Light.Address(rawAddr);

            ByteBuffer tmpBuf = payload.duplicate();
            tmpBuf.position(pos + 26);
            tmpBuf.limit(pos + 26 + 16);
            CharBuffer nameBuf = getCharset().decode(tmpBuf);
            mLogger.info("tmpBuf " + toHexString(tmpBuf));

            String name = nameBuf.toString().trim();

            mLogger.info("light: " + a + " " + addr + " '" + name + "'");

            Light light;
            if (old_lights.containsKey(addr))
                light = old_lights.get(addr);
            else
                light = new Light(this, addr, name);

            payload.position(pos + 10);
            // Format: <Q2BH4B

            byte[] b = new byte[8];
            payload.get(b);

            payload.position(pos + 10);
            byte lightType = payload.get();
            payload.position(pos + 15);
            byte onlineStatus = payload.get();
 
            payload.position(pos + 18);
            boolean on = payload.get() != 0;
            byte lum = payload.get();
            short temp = payload.getShort();
            byte red = payload.get();
            byte green = payload.get();
            byte blue = payload.get();
            byte alpha = payload.get();
            mLogger.info("status: " + toHexString(b));

            mLogger.info("type:   " + lightType);
            mLogger.info("online: " + onlineStatus);
            mLogger.info("onoff:  " + on);
            mLogger.info("temp:   " + temp);
            mLogger.info("lum:    " + lum);
            mLogger.info("red:    " + red);
            mLogger.info("green:  " + green);
            mLogger.info("blue:   " + blue);

            light.updateStatus(on, lum, temp, red, green, blue);
            new_lights.put(addr, light);
            // return (on, lum, temp, red, green, blue);

        }

        mLights = new_lights;
    }
}
