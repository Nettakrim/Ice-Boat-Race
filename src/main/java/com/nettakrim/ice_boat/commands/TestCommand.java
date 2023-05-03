package com.nettakrim.ice_boat.commands;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.joml.Vector2f;

import com.nettakrim.ice_boat.FloatMath;
import com.nettakrim.ice_boat.paths.BezierPath;
import com.nettakrim.ice_boat.paths.End;
import com.nettakrim.ice_boat.paths.Path;
import com.nettakrim.ice_boat.paths.Path.Approximation;

public class TestCommand implements CommandExecutor {
    private Random random = new Random();
    private int height = 256;
    private ArrayList<Path> paths = new ArrayList<Path>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            int size = paths.size();
            End lastEnd = null;
            if (size == 0) {
                lastEnd = new End(new Vector2f(0,0), new Vector2f(1,0));
            } else {
                lastEnd = paths.get(size-1).exit;
            }

            int attempts = 0;
            int maxAttempts = 25;
            BezierPath bezierPath = null;

            float turnWidth = 50;
            float safeZone = 50;

            while(attempts < maxAttempts) {
                bezierPath = BezierPath.buildRandom(random, lastEnd);
                Approximation approximation = bezierPath.getApproximation();
                if (size >= 1) {
                    float minimumDistance = approximation.minimumDistance(paths.get(size-1).getApproximation());
                    if (size >= 2) {
                        minimumDistance = Math.min(minimumDistance, approximation.minimumDistance(paths.get(size-2).getApproximation()));
                    }

                    Vector2f v = new Vector2f(bezierPath.entrance.point).normalize();
                    float angle = v.dot(new Vector2f(new Vector2f(bezierPath.exit.point).sub(new Vector2f(bezierPath.entrance.point))).normalize());
                    float length = bezierPath.exit.point.length();

                    attempts++;
                    if (minimumDistance < 5*2 || angle > FloatMath.clamp(1-(length-safeZone)/turnWidth, -0.5f, 1)) {
                        if (attempts == maxAttempts) height--;
                        continue;
                    }
                }
                break;
            }
            bezierPath.generate(player.getWorld(), 5, height);
            paths.add(bezierPath);

            height--;
        }

        return true;
    }
}
