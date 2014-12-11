import java.io.IOException;
import java.net.InetAddress;

import se.m7n.lightify.Connection;
import se.m7n.lightify.Luminary;

public class Lightify
{
    public static void main(String[] args) throws IOException
    {
        System.out.println("Lightify");

        String addr = args[0];
        String groupname = args[1];
        String command = args[2];

        //print "'%s' '%s' '%s'" % (addr, groupname, command)

        Connection conn = new Connection(InetAddress.getByName(addr));

        conn.updateAllLightStatus();
        conn.updateGroupList();

        //#item = conn.groups()[groupname];
        Luminary item = conn.lightByName(groupname);

        if (command.equals("on"))
            item.setOnOff(true);
        else if (command.equals("off"))
            item.setOnOff(false);
        else if (command.equals("lum")) {
            byte lum = Byte.parseByte(args[3]);
            short time = Short.parseShort(args[4]);

            item.setLuminance(lum, time);
        } else if (command.equals("temp")) {
            short temp = Short.parseShort(args[3]);
            short time = Short.parseShort(args[4]);

            item.setTemperature(temp, time);
        } else if (command.equals("col")) {
            short r = Short.parseShort(args[3]);
            short g = Short.parseShort(args[4]);
            short b = Short.parseShort(args[5]);
            short time = Short.parseShort(args[6]);

            item.setRgb((byte)r, (byte)g, (byte)b, time);
        }
        //#conn.update_light_status(item)
        return;
    }
}