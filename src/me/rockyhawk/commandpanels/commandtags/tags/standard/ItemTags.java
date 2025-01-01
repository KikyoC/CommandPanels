package me.rockyhawk.commandpanels.commandtags.tags.standard;

import eu.cloudnetservice.driver.inject.InjectionLayer;
import eu.cloudnetservice.driver.provider.CloudServiceProvider;
import eu.cloudnetservice.driver.service.ServiceInfoSnapshot;
import eu.cloudnetservice.modules.bridge.BridgeDocProperties;
import eu.cloudnetservice.modules.bridge.player.CloudPlayer;
import eu.cloudnetservice.modules.bridge.player.PlayerManager;
import eu.cloudnetservice.modules.bridge.player.executor.PlayerExecutor;
import eu.cloudnetservice.wrapper.configuration.WrapperConfiguration;
import me.rockyhawk.commandpanels.CommandPanels;
import me.rockyhawk.commandpanels.commandtags.CommandTagEvent;
import me.rockyhawk.commandpanels.openpanelsmanager.PanelPosition;
import org.bukkit.NamespacedKey;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import eu.cloudnetservice.driver.registry.ServiceRegistry;

import java.util.*;

public class ItemTags implements Listener {
    CommandPanels plugin;
    public ItemTags(CommandPanels pl) {
        this.plugin = pl;
    }

    @EventHandler
    public void commandTag(CommandTagEvent e){
        if(e.name.equalsIgnoreCase("give-item=")){
            e.commandTagUsed();
            ItemStack itm = plugin.itemCreate.makeCustomItemFromConfig(null,e.pos,e.panel.getConfig().getConfigurationSection("custom-item." + e.args[0]), e.p, true, true, false);
            if(e.args.length == 2){
                try{
                    itm.setAmount(Integer.parseInt(e.args[1]));
                } catch (Exception err){
                    plugin.debug(err,e.p);
                }
            }
            plugin.inventorySaver.addItem(e.p,itm);
            return;
        }
        if(e.name.equalsIgnoreCase("setitem=")){
            e.commandTagUsed();
            //if player uses setitem= [custom item] [slot] [position] it will change the item slot to something, used for placeable items
            //make a section in the panel called "custom-item" then whatever the title of the item is, put that here
            ItemStack s = plugin.itemCreate.makeItemFromConfig(null, e.pos,e.panel.getConfig().getConfigurationSection("custom-item." + e.args[0]), e.p, true, true, false);
            PanelPosition position = PanelPosition.valueOf(e.args[2]);
            if(position == PanelPosition.Top) {
                e.p.getOpenInventory().getTopInventory().setItem(Integer.parseInt(e.args[1]), s);
            }else if(position == PanelPosition.Middle) {
                e.p.getInventory().setItem(Integer.parseInt(e.args[1])+9, s);
            }else{
                e.p.getInventory().setItem(Integer.parseInt(e.args[1]), s);
            }
            return;
        }
        if(e.name.equalsIgnoreCase("enchant=")){
            e.commandTagUsed();
            //if player uses enchant= [slot] [position] [ADD/REMOVE/CLEAR] [enchant] <level> it will add/remove/clear the enchants of the selected slot
            PanelPosition position = PanelPosition.valueOf(e.args[1]);
            ItemStack EditItem;
            if(position == PanelPosition.Top) {
                EditItem = e.p.getOpenInventory().getTopInventory().getItem(Integer.parseInt(e.args[0]));
            }else if(position == PanelPosition.Middle) {
                EditItem = e.p.getInventory().getItem(Integer.parseInt(e.args[0])+9);
            }else{
                EditItem = e.p.getInventory().getItem(Integer.parseInt(e.args[0]));
            }

            assert EditItem != null;

            if(e.args[2].equalsIgnoreCase("add")){
                try{
                    Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(e.args[3].toLowerCase()));
                    assert enchant != null;
                    EditItem.addEnchantment(enchant, Integer.parseInt(e.args[4]));
                    return;
                } catch (Exception err){
                    plugin.debug(err,e.p);
                }
            }

            if(e.args[2].equalsIgnoreCase("remove")){
                try{
                    Enchantment enchant = Enchantment.getByKey(NamespacedKey.minecraft(e.args[3].toLowerCase()));
                    assert enchant != null;
                    EditItem.removeEnchantment(enchant);
                    return;
                } catch (Exception err){
                    plugin.debug(err,e.p);
                }
            }

            if(e.args[2].equalsIgnoreCase("clear")){
                try{
                    Set<Enchantment> Enchants = EditItem.getEnchantments().keySet();
                    for(Enchantment enchant : Enchants){
                        EditItem.removeEnchantment(enchant);
                    }
                } catch (Exception err){
                    plugin.debug(err,e.p);
                }
            }

            return;
        }
        if(e.name.equalsIgnoreCase("setcustomdata=")){
            e.commandTagUsed();
            //if player uses setcustomdata= [slot] [position] [data] it will change the custom model data of the item
            PanelPosition position = PanelPosition.valueOf(e.args[1]);
            ItemStack editItem;
            if(position == PanelPosition.Top) {
                editItem = e.p.getOpenInventory().getTopInventory().getItem(Integer.parseInt(e.args[0]));
            }else if(position == PanelPosition.Middle) {
                editItem = e.p.getInventory().getItem(Integer.parseInt(e.args[0])+9);
            }else{
                editItem = e.p.getInventory().getItem(Integer.parseInt(e.args[0]));
            }

            try{
                ItemMeta itemMeta = editItem.getItemMeta();
                itemMeta.setCustomModelData(Integer.valueOf(e.args[2]));
                editItem.setItemMeta(itemMeta);
            } catch (Exception err){
                plugin.debug(err,e.p);
            }

            return;
        }

        if (e.name.equalsIgnoreCase("tasks=")) {
            e.commandTagUsed();
            CloudServiceProvider provider = InjectionLayer.ext().instance(CloudServiceProvider.class);
            WrapperConfiguration configuration = InjectionLayer.ext().instance(WrapperConfiguration.class);

            if (provider == null || configuration == null) {
                plugin.getLogger().severe("CloudNet is not available. Make sure that cloudnet bridge module is enabled.");
                plugin.getLogger().severe("Run \"modules install CloudNet-Bridge\" in cloudnet terminal.");
                return ;
            }

            List<ServiceInfoSnapshot> task = new ArrayList<>(provider.servicesByTask(e.args[0]));
            if (task.isEmpty()) {
                plugin.getLogger().severe("Service " + e.args[0] + " not found.");
                return;
            }

            ServiceInfoSnapshot service = null;

            if (e.args.length == 1)
                service = provider.serviceByName(task.getFirst().name());
            else {
                Optional<ServiceInfoSnapshot> optional = getServiceInfoSnapshot(task, e.args[1], e.p);
                if (optional.isPresent())
                    service = optional.get();
            }
            ServiceRegistry registry = InjectionLayer.ext().instance(ServiceRegistry.class);
            if (registry == null) {
                plugin.getLogger().severe("ServiceRegistry is not available.");
                return;
            }

            PlayerManager manager = registry.firstProvider(PlayerManager.class);

            if (manager == null) {
                plugin.getLogger().severe("PlayerManager is not available.");
                return;
            }

            CloudPlayer player = manager.onlinePlayer(e.p.getUniqueId());
            if (player == null)
                return ;

            PlayerExecutor executor = manager.playerExecutor(player.uniqueId());

            if (service != null)
                executor.connect(service.name());
            return;
        }
    }

    public Optional<ServiceInfoSnapshot> getServiceInfoSnapshot(List<ServiceInfoSnapshot> task, String type, Player player) {
        System.out.println("type: " + type);
        return switch (type.toUpperCase()) {
            case "LOWEST_PLAYERS" -> task.stream()
                    .filter(service -> player.hasPermission(service.readProperty(BridgeDocProperties.REQUIRED_PERMISSION)))
                    .min(Comparator.comparingInt(service -> service.readProperty(BridgeDocProperties.ONLINE_COUNT)));
            case "HIGHEST_PLAYERS" -> task.stream()
                    .filter(service -> player.hasPermission(service.readProperty(BridgeDocProperties.REQUIRED_PERMISSION)))
                    .max(Comparator.comparingInt(service -> service.readProperty(BridgeDocProperties.ONLINE_COUNT)));
            case "RANDOM" -> Optional.of(task.get(new Random().nextInt(task.size())));
            default -> Optional.empty();
        };
    }
}
