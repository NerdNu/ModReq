package nu.nerd.modreq.utils;

import org.bukkit.Location;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import static org.bukkit.Bukkit.getServer;

public final class DataUtils {

    private DataUtils() {}

    public static Location stringToLocation(String requestLocation) {
        String[] split = requestLocation.split(",");
        String world = split[0];
        double x = Double.parseDouble(split[1]);
        double y = Double.parseDouble(split[2]);
        double z = Double.parseDouble(split[3]);

        if (split.length > 4) {
            float yaw = Float.parseFloat(split[4]);
            float pitch = Float.parseFloat(split[5]);
            return new Location(getServer().getWorld(world), x, y, z, yaw, pitch);
        } else {
            return new Location(getServer().getWorld(world), x, y, z);
        }
    }

    public static String timestampToDateString(long timestamp, String dateFormat) {
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(timestamp);
        SimpleDateFormat format = new SimpleDateFormat(dateFormat);
        return format.format(cal.getTime());
    }

}
