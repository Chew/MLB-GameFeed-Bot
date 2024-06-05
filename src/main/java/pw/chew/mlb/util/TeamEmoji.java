package pw.chew.mlb.util;

import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.Emoji;
import pw.chew.mlb.MLBBot;

/**
 * Emoii come from a server, so it may not render if the bot is not in that server!
 *
 * TODO: A better way to handle these, this is a mess.
 */
public class TeamEmoji {
    // can't be instantiated
    private TeamEmoji() {};

    // emoji by division, alphabetically so people don't get mad lol

    // AMERICAN LEAGUE WEST //
    public final static CustomEmoji ANGELS = Emoji.fromCustom("angels", 972745155897614406L, false);
    public final static CustomEmoji ASTROS = Emoji.fromCustom("astros", 972745155000012800L, false);
    public final static CustomEmoji ATHLETICS = Emoji.fromCustom("athletics", 972745154542862346L, false);
    public final static CustomEmoji MARINERS = Emoji.fromCustom("mariners", 972745156082139156L, false);
    public final static CustomEmoji RANGERS = Emoji.fromCustom("rangers", 972745153196470292L, false);

    // AMERICAN LEAGUE EAST //
    public final static CustomEmoji BLUE_JAYS = Emoji.fromCustom("bluejays", 1029965913874780160L, false);
    public final static CustomEmoji RAYS = Emoji.fromCustom("rays", 1148663077810737213L, false);
    public final static CustomEmoji RED_SOX = Emoji.fromCustom("redsox", 972745153129361448L, false);
    public final static CustomEmoji ORIOLES = Emoji.fromCustom("orioles", 1029965327989211237L, false);
    public final static CustomEmoji YANKEES = Emoji.fromCustom("yankees", 1029965894723588136L, false);

    // AMERICAN LEAGUE CENTRAL //
    public final static CustomEmoji GUARDIANS = Emoji.fromCustom("guardians", 1029965874662219826L, false);
    public final static CustomEmoji ROYALS = Emoji.fromCustom("royals", 1029965884825014322L, false);
    public final static CustomEmoji TIGERS = Emoji.fromCustom("tigers", 1029965877866664000L, false);
    public final static CustomEmoji TWINS = Emoji.fromCustom("twins", 1046992241786376272L, false);
    public final static CustomEmoji WHITE_SOX = Emoji.fromCustom("whitesox", 1029965693422149632L, false);

    // NATIONAL LEAGUE WEST //
    public final static CustomEmoji DBACKS = Emoji.fromCustom("dbacks", 972745938651213884L, false);
    public final static CustomEmoji DODGERS = Emoji.fromCustom("dodgers", 1029965941292933132L, false);
    public final static CustomEmoji GIANTS = Emoji.fromCustom("giants", 972745932825329684L, false);
    public final static CustomEmoji PADRES = Emoji.fromCustom("padres", 1029965965758312459L, false);
    public final static CustomEmoji ROCKIES = Emoji.fromCustom("rockies", 1029965938210119740L, false);

    // NATIONAL LEAGUE EAST //
    public final static CustomEmoji BRAVES = Emoji.fromCustom("braves", 1029965927812452412L, false);
    public final static CustomEmoji MARLINS = Emoji.fromCustom("marlins", 972745926378664006L, false);
    public final static CustomEmoji METS = Emoji.fromCustom("mets", 972745935853613076L, false);
    public final static CustomEmoji NATIONALS = Emoji.fromCustom("nationals", 972745941247459338L, false);
    public final static CustomEmoji PHILLIES = Emoji.fromCustom("phillies", 972745932154236999L, false);

    // NATIONAL LEAGUE CENTRAL //
    public final static CustomEmoji BREWERS = Emoji.fromCustom("brewers", 972745936910565426L, false);
    public final static CustomEmoji CUBS = Emoji.fromCustom("cubs", 1029965930995916882L, false);
    public final static CustomEmoji CARDINALS = Emoji.fromCustom("cardinals", 1029965972393693236L, false);
    public final static CustomEmoji PIRATES = Emoji.fromCustom("pirates", 1029965962952331286L, false);
    public final static CustomEmoji REDS = Emoji.fromCustom("reds", 1029965934594642000L, false);

    // UNKNOWN ?? Placeholder too //
    public final static CustomEmoji UNKNOWN = Emoji.fromCustom("unknown", 531601549668122654L, false);

    /**
     * Gets an emoji based on the team name. This is the FULL name, e.g Texas Rangers
     *
     * @return the emoji
     */
    public static CustomEmoji fromName(String input) {
        // only the production bot has access to the full list
        if (!MLBBot.jda.getSelfUser().getId().equals("987144502374436895")) {
            return UNKNOWN;
        }

        return switch (input) {
            case "Arizona Diamondbacks" -> DBACKS;
            case "Atlanta Braves" -> BRAVES;
            case "Baltimore Orioles" -> ORIOLES;
            case "Boston Red Sox" -> RED_SOX;
            case "Chicago Cubs" -> CUBS;
            case "Chicago White Sox" -> WHITE_SOX;
            case "Cincinnati Reds" -> REDS;
            case "Cleveland Guardians" -> GUARDIANS;
            case "Colorado Rockies" -> ROCKIES;
            case "Detroit Tigers" -> TIGERS;
            case "Houston Astros" -> ASTROS;
            case "Kansas City Royals" -> ROYALS;
            case "Los Angeles Angels" -> ANGELS;
            case "Los Angeles Dodgers" -> DODGERS;
            case "Miami Marlins" -> MARLINS;
            case "Milwaukee Brewers" -> BREWERS;
            case "Minnesota Twins" -> TWINS;
            case "New York Mets" -> METS;
            case "New York Yankees" -> YANKEES;
            case "Oakland Athletics" -> ATHLETICS;
            case "Philadelphia Phillies" -> PHILLIES;
            case "Pittsburgh Pirates" -> PIRATES;
            case "San Diego Padres" -> PADRES;
            case "San Francisco Giants" -> GIANTS;
            case "Seattle Mariners" -> MARINERS;
            case "St. Louis Cardinals" -> CARDINALS;
            case "Tampa Bay Rays" -> RAYS;
            case "Texas Rangers" -> RANGERS;
            case "Toronto Blue Jays" -> BLUE_JAYS;
            case "Washington Nationals" -> NATIONALS;
            default -> UNKNOWN;
        };
    }

    /**
     * Gets an emoji based on the club name. This is the "Rangers" in "Texas Rangers"
     * @param input the club name
     * @return the emoji
     */
    public static Emoji fromClubName(String input) {
        // only the production bot has access to the full list
        if (!MLBBot.jda.getSelfUser().getId().equals("987144502374436895")) {
            return UNKNOWN;
        }

        return switch (input) {
            case "Angels" -> ANGELS;
            case "Astros" -> ASTROS;
            case "Athletics" -> ATHLETICS;
            case "Blue Jays" -> BLUE_JAYS;
            case "Braves" -> BRAVES;
            case "Brewers" -> BREWERS;
            case "Cardinals" -> CARDINALS;
            case "Cubs" -> CUBS;
            case "Diamondbacks" -> DBACKS;
            case "Dodgers" -> DODGERS;
            case "Giants" -> GIANTS;
            case "Guardians" -> GUARDIANS;
            case "Mariners" -> MARINERS;
            case "Marlins" -> MARLINS;
            case "Mets" -> METS;
            case "Nationals" -> NATIONALS;
            case "Orioles" -> ORIOLES;
            case "Padres" -> PADRES;
            case "Phillies" -> PHILLIES;
            case "Pirates" -> PIRATES;
            case "Rangers" -> RANGERS;
            case "Rays" -> RAYS;
            case "Red Sox" -> RED_SOX;
            case "Reds" -> REDS;
            case "Rockies" -> ROCKIES;
            case "Royals" -> ROYALS;
            case "Tigers" -> TIGERS;
            case "Twins" -> TWINS;
            case "White Sox" -> WHITE_SOX;
            case "Yankees" -> YANKEES;
            default -> UNKNOWN;
        };
    }
}
