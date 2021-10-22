package space.gorogoro.viewdistance;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.java.JavaPlugin;

public class ViewDistance extends JavaPlugin {

  @Override
  public void onEnable(){
    try{
      getLogger().info("The Plugin Has Been Enabled!");
    } catch (Exception e) {
      logStackTrace(e);
    }
  }

  /**
   * JavaPlugin method onCommand.
   */
  public boolean onCommand( CommandSender sender, Command command, String label, String[] args) {
    // Return true:Success false:Show the usage set in plugin.yml
    try{
      if(!command.getName().equals("viewdistance")) {
        sendMsg(sender, "That command is not available.");
        return true;
      }

      if(!sender.isOp()) {
        sendMsg(sender, "No operator permissions.");
        return true;
      }

      if(args.length == 0){
        for(World w:getServer().getWorlds()) {
          sendMsg(sender, "Map: " + w.getName() + " view-distance: " + w.getViewDistance());
        }
        return true;
      }

      if(args.length != 2){
        return false;
      }

      Pattern pattern = Pattern.compile("^[0-9a-z_]+$");
      Matcher matcher = pattern.matcher(args[0]);
      if(!matcher.matches()) {
        sendMsg(sender, "The world name should be a character from 0-9a-z_.");
        return true;
      }

      World w = getServer().getWorld(args[0]);
      if(w == null) {
        sendMsg(sender, "The specified world cannot be found.");
        return true;
      }

      if(!isNumber(args[1])) {
        sendMsg(sender, "The view distance must be a number.");
        return true;
      }
      int newDistance = Integer.parseInt(args[1]);

      if(newDistance < 3 || newDistance > 32) {
        sendMsg(sender, "The view distance must be a number between 3 and 32.");
        return true;
      }

      if(w.getViewDistance() == newDistance) {
        sendMsg(sender, "There is no change in the view distance.");
        return true;
      }

      // Server side.
      Class<?> world = Class.forName("org.bukkit.World");
      Method setViewDistance = world.getDeclaredMethod("setViewDistance", int.class);
      setViewDistance.invoke(w, newDistance);

      // Client side rerender.
      Class<?> PacketPlayOutViewDistance = Class.forName("net.minecraft.network.protocol.game.PacketPlayOutViewDistance");
      Constructor<?> c = PacketPlayOutViewDistance.getConstructor(Integer.TYPE);
      Object packet = c.newInstance(newDistance);
      for (Player p : w.getPlayers()) {
        sendPacket(p, packet);
      }
      sendMsg(sender, "Changed the view distance.");

    }catch(Exception e){
      logStackTrace(e);
    }
    return true;
  }

  /**
   * JavaPlugin method onDisable.
   */
  @Override
  public void onDisable() {
    try {
      getLogger().info("The Plugin Has Been Disabled!");
    } catch (Exception e) {
      logStackTrace(e);
    }
  }

  /**
   * Send message with output console log.
   * @param CommandSender sender
   * @param String msg
   * @return void
   */
  private void sendMsg(CommandSender sender, String msg){
    getLogger().info(msg);
    sender.sendMessage(ChatColor.GRAY + msg + ChatColor.RESET);
  }

  /**
   * Check for numeric strings.
   * @param String str
   * @return boolean true：numeric false：non numeric
   */
  private static boolean isNumber(String str) {
    try {
      Integer.parseInt(str);
      return true;
    } catch (NumberFormatException e) {
      return false;
    }
  }

  /**
   * Send packet.
   * @param Player player
   * @param Object packet
   * @return void
   */
  private void sendPacket(Player player, Object packet) {
    try {
      Object nmsPlayer = player.getClass().getMethod("getHandle").invoke(player);
      Object connection = nmsPlayer.getClass().getField("b").get(nmsPlayer);
      connection.getClass().getMethod(
        "sendPacket",
        Class.forName("net.minecraft.network.protocol.Packet")
      ).invoke(
        connection,
        packet
      );
    } catch (Exception e) {
      logStackTrace(e);
    }
  }

  /**
   * Output stack trace to log file.
   * @param Exception Exception
   * @return void
   */
  private void logStackTrace(Exception e){
      StringWriter sw = new StringWriter();
      PrintWriter pw = new PrintWriter(sw);
      e.printStackTrace(pw);
      pw.flush();
      getLogger().warning(sw.toString());
  }
}