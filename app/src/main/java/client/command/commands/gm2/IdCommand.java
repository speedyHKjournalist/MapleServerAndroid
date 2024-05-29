package client.command.commands.gm2;

import client.Character;
import client.Client;
import client.command.Command;
import constants.game.NpcChat;
import constants.id.NpcId;
import server.ThreadManager;
import tools.exceptions.IdTypeNotSupportedException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class IdCommand extends Command {
    {
        setDescription("Search in handbook.");
    }
    private final static int MAX_SEARCH_HITS = 100;
    private final Map<String, String> handbookDirectory = typeFilePaths();
    private final Map<String, HandbookFileItems> typeItems = new ConcurrentHashMap<>();

    private Map<String, String> typeFilePaths() {
        return Map.ofEntries(
                Map.entry("map", "handbook/Map.txt"),
                Map.entry("etc", "handbook/Etc.txt"),
                Map.entry("npc", "handbook/NPC.txt"),
                Map.entry("use", "handbook/Use.txt"),
                Map.entry("weapon", "handbook/Equip/Weapon.txt") // TODO add more into this
        );
    }

    private static class HandbookFileItems {
        private final List<HandbookItem> items;

        public HandbookFileItems(List<String> fileLines) {
            List<HandbookItem> itemList = new ArrayList<>();

            for (String line : fileLines) {
                HandbookItem item = parseLine(line);
                if (item != null) {
                    itemList.add(item);
                }
            }

            this.items = itemList;
        }

        private HandbookItem parseLine(String line) {
            if (line == null) {
                return null;
            }

            String[] splitLine = line.split(" - ", 2);
            if (splitLine.length < 2 || splitLine[1].isBlank()) {
                return null;
            }
            return new HandbookItem(splitLine[0], splitLine[1]);
        }

        public List<HandbookItem> search(String query) {
            if (query == null || query.isBlank()) {
                return Collections.emptyList();
            }

            List<HandbookItem> result = new ArrayList<>();
            for (HandbookItem item : items) {
                if (item.matches(query)) {
                    result.add(item);
                }
            }
            return result;
        }


    }

    private record HandbookItem(String id, String name) {

        public HandbookItem {
            Objects.requireNonNull(id);
            Objects.requireNonNull(name);
        }

        public boolean matches(String query) {
            if (query == null) {
                return false;
            }
            return this.name.toLowerCase().contains(query.toLowerCase());
        }
    }

    @Override
    public void execute(Client client, final String[] params) {
        final Character chr = client.getPlayer();
        if (params.length < 2) {
            chr.yellowMessage("Syntax: !id <type> <query>");
            return;
        }
        final String type = params[0].toLowerCase();
        final String[] queryItems = Arrays.copyOfRange(params, 1, params.length);
        final String query = String.join(" ", queryItems);
        chr.yellowMessage("Querying for entry... May take some time... Please try to refine your search.");
        Runnable queryRunnable = () -> {
            try {
                populateIdMap(type);

                final HandbookFileItems handbookFileItems = typeItems.get(type);
                if (handbookFileItems == null) {
                    return;
                }
                final List<HandbookItem> searchHits = handbookFileItems.search(query);

                if (!searchHits.isEmpty()) {
                    StringBuilder searchHitsTextBuilder = new StringBuilder();
                    int count = 0;
                    for (HandbookItem item : searchHits) {
                        if (count >= MAX_SEARCH_HITS) {
                            break;
                        }

                        String line = "Id for " + item.name + " is: #" + item.id + "#k";
                        if (count > 0) {
                            searchHitsTextBuilder.append(NpcChat.NEW_LINE);
                        }
                        searchHitsTextBuilder.append(line);

                        count++;
                    }
                    String searchHitsText = searchHitsTextBuilder.toString();
                    int hitsCount = Math.min(searchHits.size(), MAX_SEARCH_HITS);
                    String summaryText = String.format("Results found: %s | Returned: %s/100 | Refine search query to improve time.",
                            searchHits.size(), hitsCount);
                    String fullText = searchHitsText + NpcChat.NEW_LINE + summaryText;
                    chr.getAbstractPlayerInteraction().npcTalk(NpcId.MAPLE_ADMINISTRATOR, fullText);
                } else {
                    chr.yellowMessage(String.format("Id not found for item: %s, of type: %s.", query, type));
                }
            } catch (IdTypeNotSupportedException e) {
                chr.yellowMessage("Your query type is not supported.");
            } catch (IOException e) {
                chr.yellowMessage("Error reading file, please contact your administrator.");
            }
        };

        ThreadManager.getInstance().newTask(queryRunnable);
    }

    private void populateIdMap(String type) throws IdTypeNotSupportedException, IOException {
        final String filePath = handbookDirectory.get(type);
        if (filePath == null) {
            throw new IdTypeNotSupportedException();
        }
        if (typeItems.containsKey(type)) {
            return;
        }

        final List<String> fileLines = Files.readAllLines(Paths.get(filePath));
        typeItems.put(type, new HandbookFileItems(fileLines));
    }
}
