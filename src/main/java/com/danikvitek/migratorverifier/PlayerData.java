package com.danikvitek.migratorverifier;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import jodd.http.HttpRequest;
import jodd.http.HttpResponse;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.model.group.Group;
import net.luckperms.api.node.Node;
import net.luckperms.api.node.types.InheritanceNode;
import org.bukkit.entity.Player;

import java.io.IOException;
import java.io.StringReader;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.stream.Collectors;

public class PlayerData {
    private JsonObject textureValueMojang;
    private final Player player;

    private final Boolean isWearingMigratorsCape;
    private final Boolean isMigrated;


    public PlayerData(Player player) throws ExecutionException, InterruptedException {
        this.player = player;
        HttpResponse response = HttpRequest.get("https://sessionserver.mojang.com/session/minecraft/profile/" + player.getUniqueId()).send();
        JsonReader reader = new JsonReader(new StringReader(response.bodyText()));
        reader.setLenient(true);
        JsonObject playerDataJSONMojang = new JsonParser().parse(reader).getAsJsonObject();
        JsonArray properties = playerDataJSONMojang.getAsJsonArray("properties");
        for (JsonElement property: properties) {
            if (property.getAsJsonObject().get("name").getAsString().equals("textures")) {
                textureValueMojang = new JsonParser().parse(decodeBase64(property.getAsJsonObject().get("value").getAsString())).getAsJsonObject();
                break;
            }
        }
        isWearingMigratorsCape = textureValueMojang.get("textures").getAsJsonObject().get("CAPE") != null && textureValueMojang.get("textures").getAsJsonObject().get("CAPE").getAsJsonObject().get("url").getAsString().equals("http://textures.minecraft.net/texture/2340c0e03dd24a11b15a8b33c2a7e9e32abb2051b2481d0ba7defd635ca7a933");

        List<String> migratedPlayers = MigratorVerifier.getModifyMigratedListFile().getStringList("players"); //.stream().map(UUID::fromString).collect(Collectors.toList());
        int playerInsertIndex = binarySearchUUID(migratedPlayers, player.getUniqueId()); // Arrays.binarySearch(migratedPlayers.toArray(), player.getUniqueId());
        if (playerInsertIndex >= 0)
            isMigrated = true;
        else if (isWearingMigratorsCape) {
            isMigrated = true;
            migratedPlayers.add(-(playerInsertIndex + 1), String.valueOf(player.getUniqueId()));
            MigratorVerifier.getModifyMigratedListFile().set("players", migratedPlayers.stream().map(String::valueOf).collect(Collectors.toList()));
            try {
                if (!MigratorVerifier.getMigratedListFile().exists())
                    MigratorVerifier.getMigratedListFile().createNewFile();
                MigratorVerifier.getModifyMigratedListFile().save(MigratorVerifier.getMigratedListFile());
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        else
            isMigrated = false;

        LuckPerms lpAPI = MigratorVerifier.getLuckPermsAPI();
        if (isMigrated && lpAPI != null) {
            Group migratorGroup = lpAPI.getGroupManager().createAndLoadGroup("migrator").get();
            lpAPI.getGroupManager().saveGroup(migratorGroup);
            lpAPI.getUserManager().modifyUser(player.getUniqueId(), user -> {
                user.data().add(InheritanceNode.builder(migratorGroup).build());
                user.data().add(Node.builder("migratorverifier.migrator.verified").value(true).build());
            });
        }
    }

    public Player getPlayer() {
        return player;
    }

    public final Boolean isMigrated() {
        return isMigrated;
    }

    public final Boolean isWearingMigratorsCape() {
        return isWearingMigratorsCape;
    }

    private static String decodeBase64(String base64) {
        return new String(Base64.getDecoder().decode(base64));
    }

    private static int binarySearchUUID(List<String> list, UUID lookFor) {
        int n = list.size(),
            l = 0,
            r = n - 1,
            m = 0;
        while (l <= r) {
            m = (l + r) / 2;
            if (UUID.fromString(list.get(m)).compareTo(lookFor) < 0)
                l = m + 1;
            else if (UUID.fromString(list.get(m)).compareTo(lookFor) > 0)
                r = m - 1;
            else
                return m;
        }
        return -m - 1;
    }
}
