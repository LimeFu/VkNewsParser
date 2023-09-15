package dev.limefu.ua.vknews;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.objects.wall.WallpostFull;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitRunnable;
import org.jooq.DSLContext;

import javax.sql.DataSource;
import java.sql.Timestamp;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

public final class VkNews extends JavaPlugin {
    @Getter
    private static VkNews instance;
    @Getter
    private int groupId = 165656;
    private DatabaseConnection vkDatabase;
    private UserActor userActor;
    private VkApiClient vk;
    private Cache<String, WallpostFull> postCache;

    @Override
    public void onEnable() {
        instance = this;
        saveDefaultConfig();
        DataSource dataSource = setupDataSource();
        DatabaseConnection databaseConnection = new DatabaseConnection(dataSource);
        getCommand("news").setExecutor(new NewsCommand(vkDatabase));
        Bukkit.getPluginManager().registerEvents(new NewsCommand(vkDatabase), this);

        databaseConnection.createTableIfNotExists();

        DSLContext dslContext = databaseConnection.getDSLContext();

        postCache = Caffeine.newBuilder()
                .maximumSize(100)
                .expireAfterWrite(30, TimeUnit.MINUTES)
                .build();
        startPostFetching();
    }

    private DataSource setupDataSource() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:mysql://localhost:3306/" + getConfig().getString("database.name"));
        config.setUsername(getConfig().getString("database.username"));
        config.setPassword(getConfig().getString("database.password"));


        return new HikariDataSource(config);
    }
    public void startPostFetching() {
        int intervalInSeconds = getConfig().getInt("scheduler.delay");

        new BukkitRunnable() {
            @Override
            public void run() {
                fetchAndStorePosts();
            }
        }.runTaskTimer(this, 0L, intervalInSeconds * 20L);
    }
    private void fetchAndStorePosts() {
        int limit = 10;
        try {
            List<WallpostFull> wallPosts = vk.wall()
                    .get(userActor)
                    .ownerId(getGroupId())
                    .count(limit)
                    .execute()
                    .getItems();

            for (WallpostFull wallPost : wallPosts) {
                int postId = wallPost.getId();
                Date postDate = new Date(wallPost.getDate() * 1000L);

                vkDatabase.insertPost(String.valueOf(postId), new Timestamp(postDate.getTime()));

                postCache.put(String.valueOf(postId), wallPost);
            }
        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }
    }

}
