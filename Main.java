import java.io.*;
import java.text.DecimalFormat;
import java.time.Instant;
import java.util.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

@SuppressWarnings("rawtypes")
public class Main {

    public static String readFile(File file) throws IOException {
        StringBuilder contentBuilder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = reader.readLine()) != null) {
                contentBuilder.append(line).append("\n");
            }
        }

        return contentBuilder.toString();
    }
    public static String decode(String input) {
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < input.length(); i++) {
            char ch = input.charAt(i);
            if (ch >= 128 || Character.isSpaceChar(ch) || ch == '/') {
                output.append("\\u").append(String.format("%04X", (int) ch).toLowerCase());
            } else {
                output.append(ch);
            }
        }

        if(output.toString().contains("\\u0020")) {
            return output.toString().replace("\\u0020", " ");
        } else {
            return output.toString();
        }
    }
    public static int countSubstrings(String mainString, String subString) {
        int count = 0;
        int index = 0;
        while ((index = mainString.indexOf(subString, index)) != -1) {
            count++;
            index += subString.length();
        }

        return count;
    }
    public static Date convertUnixTimestampToDate(long unixTimestamp) {
        return new Date(unixTimestamp * 1000); // Multiply by 1000 to convert seconds to milliseconds
    }
    public static File compileData(File file1) throws IOException {

        String mainPath = file1 + "/";
        // getting the name of the user
        String personal_information = readFile(new File(mainPath + "personal_information/personal_information/personal_information.json"));

        JSONObject jsonData = new JSONObject(personal_information);
        JSONArray profileUserArray = jsonData.getJSONArray("profile_user");
        JSONObject profileData = profileUserArray.getJSONObject(0);
        JSONObject stringMapData = profileData.getJSONObject("string_map_data");

        JSONObject nameObject = stringMapData.getJSONObject("Name");
        // holds the USERNAME of the user (e.i. John (NOT xyz_John))
        String name = decode(nameObject.getString("value"));

        // getting all the subfolders (unique messages) within the inbox
        String messages_path = mainPath + "your_instagram_activity/messages/inbox";
        File messages_folder = new File(messages_path);

        // holds a list of all the folders within the inbox folder
        File[] directories = messages_folder.listFiles(File::isDirectory);

        // file -> # of json message files within the folder
        // creating a map with all the contents of the message.json files mapped to files
        HashMap<File, ArrayList<String>> message_content_map = new HashMap<>();
        assert directories != null;
        for (File directory : directories) {
            File[] subdirectories = directory.listFiles();
            // temporary arraylist for storing contents
            ArrayList<String> content = new ArrayList<>();
            assert subdirectories != null;
            for (File a : subdirectories) {
                // check if the file has .json (e.i. message_1.json)
                if (a.getName().contains(".json")) {
                    content.add(readFile(a));
                }
            }
            message_content_map.put(directory, content);
        }

        // file -> list of participant names for that unique file, no dupes for extra message.json
        HashMap<File, ArrayList> message_participant_map = new HashMap<>();
        for (File directory : directories) {
            String JSONString = message_content_map.get(directory).get(0);
            JSONObject object = new JSONObject(JSONString);
            JSONArray participantArray = object.getJSONArray("participants");
            ArrayList<String> participantList = new ArrayList<>();
            for (int j = 0; j < participantArray.length(); j++) {
                JSONObject temp = participantArray.getJSONObject(j);
                participantList.add(decode((String) temp.get("name")));
            }
            message_participant_map.put(directory, participantList);
        }

        // 2 arraylists to hold which conversations are dms and which are gcs
        ArrayList<File> dm_names = new ArrayList<>();
        ArrayList<File> gc_names = new ArrayList<>();
        // hold the contents of each one in a separate object
        ArrayList<String> dm_contents = new ArrayList<>();
        ArrayList<String> gc_contents = new ArrayList<>();
        for (File directory : directories) {
            if (message_participant_map.get(directory).size() > 2) {
                gc_names.add(directory);
                gc_contents.add(message_content_map.get(directory).toString());
            } else {
                dm_names.add(directory);
                dm_contents.add(message_content_map.get(directory).toString());
            }
        }

        // checking how many dms were sent and received in each conversation, and store usernames
        HashMap<File, Integer> dms_sentMap = new HashMap<>();
        HashMap<File, Integer> dms_receivedMap = new HashMap<>();
        HashMap<File, String> username_map = new HashMap<>();
        for (int i = 0; i < dm_names.size(); i++) {
            dms_sentMap.put(dm_names.get(i), countSubstrings(decode(dm_contents.get(i)), "\"sender_name\": \"" + name));
            dms_receivedMap.put(dm_names.get(i), countSubstrings(decode(dm_contents.get(i)), "\"sender_name\": \"" + message_participant_map.get(dm_names.get(i)).get(0)));
            username_map.put(dm_names.get(i), message_participant_map.get(dm_names.get(i)).get(0).toString());
        }

        // sorting the dms sent map
        List<Map.Entry<File, Integer>> sentEntryList = new ArrayList<>(dms_sentMap.entrySet());
        sentEntryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // sorting the dms received map
        List<Map.Entry<File, Integer>> receivedEntryList = new ArrayList<>(dms_receivedMap.entrySet());
        receivedEntryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // calculate the sum of integers in dms sent map
        int sentSum = 0;
        for (int value : dms_sentMap.values()) {
            sentSum += value;
        }

        // calculate the sum of integers in dms received map
        int receivedSum = 0;
        for (int value : dms_receivedMap.values()) {
            receivedSum += value;
        }

        ArrayList<String> topSentDMUsername = new ArrayList<>();
        ArrayList<Integer> topSentDMCount = new ArrayList<>();
        int sentCount = 0;
        for (Map.Entry<File, Integer> entry : sentEntryList) {
            if (sentCount >= 10) {
                break;
            }
            topSentDMUsername.add(username_map.get(entry.getKey()));
            topSentDMCount.add(entry.getValue());
            sentCount++;
        }

        ArrayList<String> topReceivedDMUsername = new ArrayList<>();
        ArrayList<Integer> topReceivedDMCount = new ArrayList<>();
        int receivedCount = 0;
        for (Map.Entry<File, Integer> entry : receivedEntryList) {
            if (receivedCount >= 10) {
                break;
            }
            topReceivedDMUsername.add(username_map.get(entry.getKey()));
            topReceivedDMCount.add(entry.getValue());
            receivedCount++;
        }

        // checking how many group chat messages were sent and received in each conversation, and storing group chat names
        HashMap<File, Integer> gcs_sentMap = new HashMap<>();
        HashMap<File, Integer> gcs_receivedMap = new HashMap<>();
        HashMap<File, String> gcs_name_map = new HashMap<>();
        for (int i = 0; i < gc_names.size(); i++) {
            gcs_sentMap.put(gc_names.get(i), countSubstrings(gc_contents.get(i), "\"sender_name\": \"" + name));
            gcs_receivedMap.put(gc_names.get(i), countSubstrings(gc_contents.get(i), "\"sender_name\": \"") - countSubstrings(gc_contents.get(i), "\"sender_name\": \"" + name));
            JSONArray arr = new JSONArray(gc_contents.get(i));
            JSONObject title = arr.getJSONObject(0);
            gcs_name_map.put(gc_names.get(i), decode(title.getString("title")));
        }

        // sort values from gcs sent map
        List<Map.Entry<File, Integer>> gc_sentEntryList = new ArrayList<>(gcs_sentMap.entrySet());
        gc_sentEntryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // sort values from gcs received map
        List<Map.Entry<File, Integer>> gc_receivedEntryList = new ArrayList<>(gcs_receivedMap.entrySet());
        gc_receivedEntryList.sort((entry1, entry2) -> entry2.getValue().compareTo(entry1.getValue()));

        // calculate the sum of integers in dms sent map
        int gc_sendSum = 0;
        for (int value : gcs_sentMap.values()) {
            gc_sendSum += value;
        }

        // calculate the sum of integers in dms received map
        int gc_receivedSum = 0;
        for (int value : gcs_receivedMap.values()) {
            gc_receivedSum += value;
        }

        ArrayList<String> topSentGCUsername = new ArrayList<>();
        ArrayList<Integer> topSentGCCount = new ArrayList<>();
        int gc_temporary_name_counter = 0;
        for (Map.Entry<File, Integer> entry : gc_sentEntryList) {
            if (gc_temporary_name_counter >= 5) {
                break;
            }
            topSentGCUsername.add(gcs_name_map.get(entry.getKey()));
            topSentGCCount.add(entry.getValue());
            gc_temporary_name_counter++;
        }

        ArrayList<String> topReceivedGCUsername = new ArrayList<>();
        ArrayList<Integer> topReceivedGCCount = new ArrayList<>();
        int gc_receivedCount = 0;
        for (Map.Entry<File, Integer> entry : gc_receivedEntryList) {
            if (gc_receivedCount >= 5) {
                break;
            }
            topReceivedGCUsername.add(gcs_name_map.get(entry.getKey()));
            topReceivedGCCount.add(entry.getValue());
            gc_receivedCount++;
        }

        // debug
        System.out.println("Direct Message Participants and Sent Counts:");
        for (Map.Entry<File, Integer> entry : dms_sentMap.entrySet()) {
            System.out.println("DM Name: " + username_map.get(entry.getKey()) + ", Messages Sent: " + entry.getValue());
        }

        System.out.println("Direct Message Participants and Received Counts:");
        for (Map.Entry<File, Integer> entry : dms_receivedMap.entrySet()) {
            System.out.println("DM Name: " + username_map.get(entry.getKey()) + ", Messages Received: " + entry.getValue());
        }

        System.out.println("Top Sent DM Usernames and Message Counts:");
        for (int i = 0; i < topSentDMUsername.size(); i++) {
            System.out.println("Username: " + topSentDMUsername.get(i) + ", Messages Sent: " + topSentDMCount.get(i));
        }

        System.out.println("Top Received DM Usernames and Message Counts:");
        for (int i = 0; i < topReceivedDMUsername.size(); i++) {
            System.out.println("Username: " + topReceivedDMUsername.get(i) + ", Messages Received: " + topReceivedDMCount.get(i));
        }

        System.out.println("Group Chat Participants and Sent Counts:");
        for (Map.Entry<File, Integer> entry : gcs_sentMap.entrySet()) {
            System.out.println("GC Name: " + gcs_name_map.get(entry.getKey()) + ", Messages Sent: " + entry.getValue());
        }

        System.out.println("Group Chat Participants and Received Counts:");
        for (Map.Entry<File, Integer> entry : gcs_receivedMap.entrySet()) {
            System.out.println("GC Name: " + gcs_name_map.get(entry.getKey()) + ", Messages Received: " + entry.getValue());
        }

        // MESSAGES SECTION IS NOW OVER

        // getting followers and the unix timestamp in which they followed
        HashMap<String, Long> followerMap = new HashMap<>();
        ArrayList<String> followers = new ArrayList<>();
        // IMPORTANT: AT SOME POINT MAKE IT COMPATIBLE FOR MULTIPLE FOLLOWER.JSON FOLDERS
        String followerDataString = readFile(new File(mainPath + "connections/followers_and_following/followers_1.json"));
        JSONArray followerData = new JSONArray(followerDataString);
        for (int i = 0; i < followerData.length(); i++) {
            String jsonString = followerData.get(i).toString();
            try {
                JSONObject jsonObject = new JSONObject(jsonString);
                JSONArray stringListData = jsonObject.getJSONArray("string_list_data");

                if (stringListData.length() > 0) {
                    JSONObject firstObject = stringListData.getJSONObject(0);
                    String value = firstObject.getString("value");
                    long timestamp = firstObject.getLong("timestamp");

                    followers.add(value);
                    followerMap.put(value, timestamp);
                } else {
                    System.out.println("No data found in string_list_data.");
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        List<Map.Entry<String, Long>> sortedList = new ArrayList<>(followerMap.entrySet());
        sortedList.sort(Map.Entry.comparingByValue());

        // all the same kinda stuff but for followING this time
        HashMap<String, Long> followingMap = new HashMap<>();
        ArrayList<String> following = new ArrayList<>();
        // i don't think there can be multiple following.json files (not certain)
        String followingDataString = readFile(new File(mainPath + "connections/followers_and_following/following.json"));
        JSONObject followingData = new JSONObject(followingDataString);
        JSONArray relationshipsFollowing = followingData.getJSONArray("relationships_following");

        for (int i = 0; i < relationshipsFollowing.length(); i++) {
            JSONObject entry = relationshipsFollowing.getJSONObject(i);
            JSONArray stringListData = entry.getJSONArray("string_list_data");

            for (int j = 0; j < stringListData.length(); j++) {
                JSONObject data = stringListData.getJSONObject(j);
                String value = data.getString("value");
                long timestamp = data.getLong("timestamp");

                following.add(value);
                followingMap.put(value, timestamp);
            }
        }

        ArrayList<String> followingNotFollowers = new ArrayList<>();
        ArrayList<String> followersNotFollowing = new ArrayList<>();

        for (String followee : following) {
            if (!followers.contains(followee)) {
                followingNotFollowers.add(followee);
            }
        }

        for (String follower : followers) {
            if (!following.contains(follower)) {
                followersNotFollowing.add(follower);
            }
        }

        String pending_requestsString = readFile(new File(mainPath + "connections/followers_and_following/pending_follow_requests.json"));
        int pending_requests = countSubstrings(pending_requestsString, "\"value\": ");

        ArrayList<String> dontFollow = new ArrayList<>(followingNotFollowers);

        ArrayList<String> oldestFollowers = new ArrayList<>();
        ArrayList<Date> oldestFollowersDates = new ArrayList<>();
        for (int i = 0; i < 10 && i < sortedList.size(); i++) {
            Map.Entry<String, Long> entry = sortedList.get(i);
            oldestFollowers.add(entry.getKey());
            oldestFollowersDates.add(convertUnixTimestampToDate(entry.getValue()));
        }

        // FOLLOWERS SECTION IS NOW OVER!!!!

        String storyHistoryString = readFile(new File(mainPath + "your_instagram_activity/content/stories.json"));
        JSONObject storyHistory = new JSONObject(storyHistoryString);

        JSONArray storyData_1 = storyHistory.getJSONArray("ig_stories");

        List<String> uris = new ArrayList<>();
        List<Long> creationTimestamps = new ArrayList<>();

        for (int i = 0; i < storyData_1.length(); i++) {
            JSONObject jsonObject = storyData_1.getJSONObject(i);

            uris.add(jsonObject.optString("uri", ""));
            creationTimestamps.add(jsonObject.optLong("creation_timestamp", 0));
        }

        int storyCount = uris.size();
        Collections.sort(creationTimestamps);

        // Calculate the timestamp one month ago
        long currentTime = System.currentTimeMillis() / 1000; // Convert to seconds
        long oneMonthAgo = currentTime - 30 * 24 * 60 * 60; // 30 days * 24 hours * 60 minutes * 60 seconds

        List<Date> pastMonth = new ArrayList<>();
        // Print timestamps within the past one month
        for (Long timestamp : creationTimestamps) {
            if (timestamp >= oneMonthAgo && timestamp <= currentTime) {
                Date date = new Date(timestamp * 1000); // Convert to milliseconds
                pastMonth.add(date);
            }
        }

        String signup_informationString = readFile(new File(mainPath + "security_and_login_information\\login_and_profile_creation\\instagram_signup_details.json"));
        JSONObject signup_information = new JSONObject(signup_informationString);
        long creationTimestamp = signup_information.getJSONArray("account_history_registration_info").getJSONObject(0).getJSONObject("string_map_data").getJSONObject("Time").getLong("timestamp");

        long currentTimestamp = Instant.now().getEpochSecond(); // Current UNIX timestamp
        long timeDifferenceInSeconds = currentTimestamp - creationTimestamp;
        int daysSinceCreation = (int) (timeDifferenceInSeconds / (24 * 60 * 60)); // 86400 seconds in a day
        double weeklyStories = (double) storyCount / (daysSinceCreation / 7);
        DecimalFormat decimalFormat = new DecimalFormat("#.##");
        weeklyStories = Double.parseDouble(decimalFormat.format(weeklyStories));
        double pastMonthAverage = (double) pastMonth.size() / 30;
        decimalFormat = new DecimalFormat("#.##");
        pastMonthAverage = Double.parseDouble(decimalFormat.format(pastMonthAverage));

        // STORIES SECTION IS NOW OVER

        int postsLiked = new JSONObject(readFile(new File(mainPath + "your_instagram_activity/likes/liked_posts.json"))).getJSONArray("likes_media_likes").length();
        int commentsLiked = new JSONObject(readFile(new File(mainPath + "your_instagram_activity/likes/liked_comments.json"))).getJSONArray("likes_comment_likes").length();
        // make this more flexible for more files in the future
        int totalPostComments = new JSONArray(readFile(new File(mainPath + "your_instagram_activity/comments/post_comments_1.json"))).length();
        if (new File(mainPath + "comments/post_comments_2").exists()) {
            totalPostComments += new JSONArray(readFile(new File(mainPath + "your_instagram_activity/comments/post_comments_2.json"))).length();
        }
        int totalReelsComments = new JSONObject(readFile(new File(mainPath + "your_instagram_activity/comments/reels_comments.json"))).getJSONArray("comments_reels_comments").length();
        int totalStoryLikes = new JSONObject(readFile(new File(mainPath + "your_instagram_activity/story_sticker_interactions/story_likes.json"))).getJSONArray("story_activities_story_likes").length();
        int totalPollResponses = new JSONObject(readFile(new File(mainPath + "your_instagram_activity/story_sticker_interactions/polls.json"))).getJSONArray("story_activities_polls").length();

        // main message stats
        JSONObject messages = new JSONObject();
        messages.put("core_message_stats", new JSONObject()
                .put("sent_dm", sentSum)
                .put("received_dm", receivedSum)
                .put("total_dms", dm_names.size())
                .put("sent_gc", gc_sendSum)
                .put("received_gc", gc_receivedSum)
                .put("total_gcs", gc_names.size())
        );

        // messages sent stats
        JSONArray topNamesMessageSentArray = new JSONArray();
        JSONArray topDMMessageSentArray = new JSONArray();
        for (int i = 0; i < 10; i++) {
            topNamesMessageSentArray.put(topSentDMUsername.get(i));
            topDMMessageSentArray.put(topSentDMCount.get(i));
        }

        messages.put("top_sent_users", new JSONObject()
                .put("names", topNamesMessageSentArray)
                .put("messages", topDMMessageSentArray));

        // messages received stats
        JSONArray topNamesMessageReceivedArray = new JSONArray();
        JSONArray topDMMessageReceivedArray = new JSONArray();
        for (int i = 0; i < 10; i++) {
            topNamesMessageReceivedArray.put(topReceivedDMUsername.get(i));
            topDMMessageReceivedArray.put(topReceivedDMCount.get(i));
        }

        messages.put("top_received_users", new JSONObject()
                .put("names", topNamesMessageReceivedArray)
                .put("messages", topDMMessageReceivedArray));

        // group chat sent stats
        JSONArray topGCNamesMessageSentArray = new JSONArray();
        JSONArray topGCMessageSentArray = new JSONArray();
        for (int i = 0; i < 5; i++) {
            topGCNamesMessageSentArray.put(topSentGCUsername.get(i));
            topGCMessageSentArray.put(topSentGCCount.get(i));
        }

        messages.put("top_sent_gc", new JSONObject()
                .put("names", topGCNamesMessageSentArray)
                .put("messages", topGCMessageSentArray));

        // group chat received stats
        JSONArray topGCNamesMessageReceivedArray = new JSONArray();
        JSONArray topGCMessageReceivedArray = new JSONArray();
        for (int i = 0; i < 5; i++) {
            topGCNamesMessageReceivedArray.put(topReceivedGCUsername.get(i));
            topGCMessageReceivedArray.put(topReceivedGCCount.get(i));
        }

        messages.put("top_received_gc", new JSONObject()
                .put("names", topGCNamesMessageReceivedArray)
                .put("messages", topGCMessageReceivedArray));

        // main follower stats
        JSONObject follows = new JSONObject();
        follows.put("core_follower_stats", new JSONObject()
                .put("followers", followerMap.size())
                .put("following", followingMap.size())
                .put("requested", pending_requests)
                .put("following_not_followers", followingNotFollowers.size())
                .put("following_not_followers", followersNotFollowing.size()));

        // following not followers
        JSONArray followingNotFollowersArray = new JSONArray();
        for (int i = 0; i < followingNotFollowers.size(); i++) {
            followingNotFollowersArray.put(dontFollow.get(i));
        }

        follows.put("following_not_followers", new JSONObject()
                .put("handles", followingNotFollowersArray));

        // oldest followers
        JSONArray oldestFollowersArray = new JSONArray();
        JSONArray oldestFollowerDatesArray = new JSONArray();
        for (int i = 0; i < 10; i++) {
            oldestFollowersArray.put(oldestFollowers.get(i));
            oldestFollowerDatesArray.put((oldestFollowersDates.get(i)));
        }

        follows.put("oldest_followers", new JSONObject()
                .put("handles", oldestFollowersArray)
                .put("dates", oldestFollowerDatesArray));

        // story stats
        JSONObject stories = new JSONObject();
        stories.put("core_story_stats", new JSONObject()
                .put("total", storyCount)
                .put("past_month", pastMonth.size())
                .put("weekly_avg", weeklyStories)
                .put("monthly_avg", pastMonthAverage)
                .put("first_story", convertUnixTimestampToDate(creationTimestamps.get(0))));

        // likes & interaction states
        JSONObject interactions = new JSONObject();
        interactions.put("core_interactions_stats", new JSONObject()
                .put("posts_liked", postsLiked)
                .put("comments_liked", commentsLiked)
                .put("stories_liked", totalStoryLikes)
                .put("post_comments", totalPostComments)
                .put("reels_comments", totalReelsComments)
                .put("poll_responses", totalPollResponses));

        File folder = new File("compiled_data");
        if (!folder.exists()) {
            if (folder.mkdir()) {
                System.out.println("Folder created successfully.");
            } else {
                System.err.println("Failed to create the folder.");
            }
        }

        List<JSONObject> list = new ArrayList<>();
        list.add(messages); list.add(follows); list.add(stories); list.add(interactions);
        String[] titles = {"messages", "follows", "stories", "interactions"};
        for (int i = 0; i < list.size(); i++) {
            File file = new File(folder, titles[i] + ".json");
            try (FileWriter fileWriter = new FileWriter(file)) {
                fileWriter.write(list.get(i).toString(2));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } return folder;
    }

    public static void main(String[] args) throws IOException {

        File file = new File("C:\\Users\\NUC\\OneDrive\\Desktop\\instagram-pdx.akbar-2024-11-30-xikLKA5D\\");
        compileData(file);


    }
}