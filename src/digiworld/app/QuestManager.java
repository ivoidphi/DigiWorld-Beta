package digiworld.app;

public class QuestManager {
    public static final int STAGE_GAME_START = 0;
    public static final int STAGE_TALKED_PROF = 1;
    public static final int STAGE_TALKED_GEN = 2;
    public static final int STAGE_RETURNED_PROF = 3;
    public static final int STAGE_SELECTED_STARTERS = 4;
    public static final int STAGE_ENTERED_DIGIWORLD = 5;
    public static final int STAGE_REACHED_ALPHA_VILLAGE = 6;
    public static final int STAGE_TALKED_CHIEF_REI = 7;
    public static final int STAGE_COMPLETED_TUTORIAL = 8;
    public static final int STAGE_DEFEATED_ALDRICH = 9;
    public static final int STAGE_RETURNED_TO_LAB = 10;
    public static final int STAGE_ENTERED_BETA_CITY = 11;
    public static final int STAGE_DEFEATED_ACE_JAZZ = 12;
    public static final int STAGE_ENTERED_TOURNAMENT_HALL = 13;
    public static final int STAGE_DEFEATED_TRIALMASTER = 14;
    public static final int STAGE_TOURNAMENT_STARTED = 15;
    public static final int STAGE_REACHED_GLITCH_AREA = 16;
    public static final int STAGE_DEFEATED_GLITCH = 17;
    public static final int STAGE_ENDING_TRIGGERED = 18;

    private int questStage = STAGE_GAME_START;

    public int getQuestStage() {
        return questStage;
    }

    public void setQuestStage(int stage) {
        questStage = Math.max(STAGE_GAME_START, Math.min(STAGE_ENDING_TRIGGERED, stage));
    }

    public boolean isStage(int stage) {
        return questStage == stage;
    }

    public boolean atLeast(int stage) {
        return questStage >= stage;
    }

    public String objectiveForStage() {
        return switch (questStage) {
            case STAGE_GAME_START -> "Talk to Professor Alfred";
            case STAGE_TALKED_PROF -> "Talk to General Edrian";
            case STAGE_TALKED_GEN -> "Return to Professor Alfred";
            case STAGE_RETURNED_PROF -> "Choose 3 Mecha Beasts from Professor Alfred";
            case STAGE_SELECTED_STARTERS -> "Enter DigiWorld";
            case STAGE_ENTERED_DIGIWORLD -> "Reach Alpha Village";
            case STAGE_REACHED_ALPHA_VILLAGE -> "Talk to Chief Rei";
            case STAGE_TALKED_CHIEF_REI -> "Locate the Wild Vineratops using your G-Watch";
            case STAGE_COMPLETED_TUTORIAL -> "Return to Professor Alfred";
            case STAGE_DEFEATED_ALDRICH -> "Talk to Professor Alfred";
            case STAGE_RETURNED_TO_LAB -> "Defeat Ace Trainer Jazz";
            case STAGE_ENTERED_BETA_CITY -> "Battle Ace Trainer Jazz";
            case STAGE_DEFEATED_ACE_JAZZ -> "Go to the Tournament Hall";
            case STAGE_ENTERED_TOURNAMENT_HALL -> "Challenge the Trialmaster";
            case STAGE_DEFEATED_TRIALMASTER -> "Wait for the Tournament to Begin";
            case STAGE_TOURNAMENT_STARTED -> "Find the source of the glitch";
            case STAGE_REACHED_GLITCH_AREA -> "Defeat Glitch";
            case STAGE_DEFEATED_GLITCH -> "Return to the Real World";
            default -> "TO BE CONTINUED";
        };
    }
}
