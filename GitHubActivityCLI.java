import java.net.*;
import java.io.*;
import java.util.Scanner;

public class GitHubActivityCLI {
    public static void main(String[] args) {
        String username = null;
        if (args.length == 1) {
            username = args[0];
        } else {
            System.out.print("Enter GitHub Username: ");
            Scanner sc = new Scanner(System.in);
            String input = sc.nextLine().trim();
            if (input.isEmpty()) {
                System.out.println("Usage: java GitHubActivityCLI <GitHub Username>");
                sc.close();
                return;
            }
            username = input;
            sc.close();
        }
        fetchGitHubActivity(username);
    }

    static void fetchGitHubActivity(String username) {
        String apiUrl = "https://api.github.com/users/" + username + "/events/public";
        try {
            URL url = new URL(apiUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setRequestProperty("User-Agent", "GitHubActivityCLI");
            conn.setRequestProperty("Accept", "application/vnd.github+json");
            // Optional: use token from environment to raise rate limits
            String token = System.getenv("GITHUB_TOKEN");
            if (token != null && !token.trim().isEmpty()) {
                conn.setRequestProperty("Authorization", "token " + token.trim());
            }

            int responseCode = conn.getResponseCode();
            System.out.println("HTTP " + responseCode + " " + conn.getResponseMessage());
            String limit = conn.getHeaderField("X-RateLimit-Limit");
            String remaining = conn.getHeaderField("X-RateLimit-Remaining");
            String reset = conn.getHeaderField("X-RateLimit-Reset");
            if (limit != null || remaining != null) {
                System.out.println("Rate limit: " + (remaining != null ? remaining : "?") + "/" + (limit != null ? limit : "?") + (reset != null ? " (reset: " + reset + ")" : ""));
            }

            if (responseCode == 200) {
                BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String line;
                StringBuilder response = new StringBuilder();
                while ((line = reader.readLine()) != null) {
                    response.append(line);
                }
                reader.close();
                String respStr = response.toString().trim();
                if (respStr.isEmpty() || respStr.equals("[]")) {
                    System.out.println("No public events found for user '" + username + "'.\nIf you expect activity, ensure the user has public activity or try a different username.");
                    System.out.println("Raw JSON response:\n" + respStr);
                } else {
                    System.out.println("Raw JSON response:\n" + respStr);
                }
            } else if (responseCode == 404) {
                System.out.println("User not found (404). Check the username.");
            } else if (responseCode == 403) {
                System.out.println("Access denied or rate-limited (403). Consider setting a GITHUB_TOKEN environment variable to increase rate limits.");
                // try to print error body
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder eb = new StringBuilder();
                    String l;
                    while (err != null && (l = err.readLine()) != null) eb.append(l);
                    if (eb.length() > 0) System.out.println("Error body: " + eb.toString());
                } catch (Exception ignored) {}
            } else {
                System.out.println("Failed to fetch activity! Response Code: " + responseCode);
                try (BufferedReader err = new BufferedReader(new InputStreamReader(conn.getErrorStream()))) {
                    StringBuilder eb = new StringBuilder();
                    String l;
                    while (err != null && (l = err.readLine()) != null) eb.append(l);
                    if (eb.length() > 0) System.out.println("Error body: " + eb.toString());
                } catch (Exception ignored) {}
            }
            conn.disconnect();
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
            e.printStackTrace(System.out);
        }
    }
}