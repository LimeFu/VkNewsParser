package dev.limefu.ua.vknews;

import com.vk.api.sdk.client.VkApiClient;
import com.vk.api.sdk.client.actors.UserActor;
import com.vk.api.sdk.exceptions.ApiException;
import com.vk.api.sdk.exceptions.ClientException;
import com.vk.api.sdk.httpclient.HttpTransportClient;
import com.vk.api.sdk.objects.wall.WallpostFull;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import java.util.Collections;
import java.util.Date;
import java.util.List;

public class NewsCommand implements CommandExecutor, Listener {
    private final DatabaseConnection vkDatabase;
    private UserActor userActor;

    public NewsCommand(DatabaseConnection vkDatabase) {
        this.vkDatabase = vkDatabase;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            openNewsGUI(player);
        }
        return true;
    }
    private void openNewsGUI(Player player) {
        int limit = 20;
        Inventory gui = Bukkit.createInventory(player, 27, "VK News");

        try {
            VkApiClient vk = new VkApiClient(new HttpTransportClient());
            List<WallpostFull> wallPosts = vk.wall()
                    .get(userActor)
                    .ownerId(VkNews.getInstance().getGroupId())
                    .count(limit)
                    .execute()
                    .getItems();

            for (WallpostFull wallPost : wallPosts) {
                int postId = wallPost.getId();
                Date postDate = new Date(wallPost.getDate() * 1000L);
                String postLink = "https://vk.com/" + VkNews.getInstance().getGroupId() + "?w=wall" + postId;

                ItemStack postItem = new ItemStack(Material.PAPER);
                ItemMeta meta = postItem.getItemMeta();
                meta.setDisplayName("Новость #" + postId);
                meta.setLore(Collections.singletonList("Кликните, чтобы прочитать новость"));
                postItem.setItemMeta(meta);

                gui.addItem(postItem);
            }
        } catch (ApiException | ClientException e) {
            e.printStackTrace();
        }

        player.openInventory(gui);
    }

    @EventHandler
    public void onInventoryClick(InventoryClickEvent event) {
        Player player = (Player) event.getWhoClicked();
        Inventory clickedInventory = event.getClickedInventory();

        if (clickedInventory != null && clickedInventory.getName().equals("VK News")) {
            event.setCancelled(true);

            ItemStack clickedItem = event.getCurrentItem();
            String postLink = getItemPostLink(clickedItem);
            if (clickedItem != null && clickedItem.getType() == Material.PAPER) {
                player.sendMessage("Ссылка на новость: " + postLink);
                player.closeInventory();
            }
        }
    }
    public String getItemPostLink(ItemStack item) {
        if (item != null && item.hasItemMeta()) {
            ItemMeta itemMeta = item.getItemMeta();

            if (itemMeta.hasLore()) {
                for (String loreLine : itemMeta.getLore()) {
                    if (loreLine.startsWith("Ссылка: ")) {
                        return loreLine.replace("Ссылка: ", "");
                    }
                }
            }
        }

        return null;
    }



}
