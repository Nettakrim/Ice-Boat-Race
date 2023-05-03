package com.nettakrim.ice_boat.commands;

import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.paths.BezierPath;
import com.nettakrim.ice_boat.paths.End;

public class TestCommand implements CommandExecutor {
    private End lastEnd = new End(new Vector2f(0,0), new Vector2f(1,1));
    private Random random = new Random();
    private int height = 256;

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            float entranceLength = (random.nextFloat()+1)*20f;
            Vector2f exit = new Vector2f(random.nextFloat()-0.5f, random.nextFloat()-0.5f).normalize();
            while (exit.dot(lastEnd.angle) < 0.5f) {
                exit = new Vector2f(random.nextFloat()-0.5f, random.nextFloat()-0.5f).normalize();
            }
            exit.mul((random.nextFloat()+1)*20f);
            exit.add(lastEnd.point);

            BezierPath bezierPath = BezierPath.build(lastEnd, entranceLength, exit);
            bezierPath.generate(player.getWorld(), 5, height);
            
            lastEnd = bezierPath.exit;
            height--;
        }

        return true;
    }
}
