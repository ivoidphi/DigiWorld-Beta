package digiworld.app;

public class QuestManager {
    public static final int STAGE_GAME_START = 0;
    public static final int STAGE_TALKED_PROF = 1;
    public static final int STAGE_TALKED_GEN = 2;
    public static final int STAGE_RETURNED_PROF = STAGE_TALKED_GEN;
    public static final int STAGE_SELECTED_STARTERS = 3;
    public static final int STAGE_ENTERED_DIGIWORLD = 4;
    public static final int STAGE_REACHED_ALPHA_VILLAGE = STAGE_ENTERED_DIGIWORLD;
    public static final int STAGE_TALKED_CHIEF_REI = 5;
    public static final int STAGE_COMPLETED_TUTORIAL = 6;
    public static final int STAGE_DEFEATED_ALDRICH = 7;
    public static final int STAGE_RETURNED_TO_LAB = 8;
    public static final int STAGE_ENTERED_BETA_CITY = 9;
    public static final int STAGE_DEFEATED_ACE_JAZZ = 10;
    public static final int STAGE_ENTERED_TOURNAMENT_HALL = 11;
    public static final int STAGE_DEFEATED_TRIALMASTER = 12;
    public static final int STAGE_TOURNAMENT_STARTED = 13;
    public static final int STAGE_REACHED_GLITCH_AREA = 14;
    public static final int STAGE_DEFEATED_GLITCH = 15;
    public static final int STAGE_ENDING_TRIGGERED = 16;

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
            case STAGE_GAME_START -> "Find Professor Alfred";
            case STAGE_TALKED_PROF -> "Talk to General Edrian";
            case STAGE_TALKED_GEN -> "Return to Professor Alfred";
            case STAGE_SELECTED_STARTERS -> "Go to the teleport door";
            case STAGE_ENTERED_DIGIWORLD -> "Find Chief Rei";
            case STAGE_TALKED_CHIEF_REI -> "Locate the Wild Vineratops using your G-Watch";
            case STAGE_COMPLETED_TUTORIAL -> "Reach the Heart of the Forest";
            case STAGE_DEFEATED_ALDRICH -> "Stage 2 setup in progress";
            case STAGE_RETURNED_TO_LAB -> "Defeat Ace Trainer Jazz";
            case STAGE_ENTERED_BETA_CITY -> "Go to the Tournament Hall";
            case STAGE_DEFEATED_ACE_JAZZ -> "Wait for the Tournament to Begin";
            case STAGE_ENTERED_TOURNAMENT_HALL -> "Challenge Trialmaster";
            case STAGE_DEFEATED_TRIALMASTER -> "Wait for the Tournament to Begin";
            case STAGE_TOURNAMENT_STARTED -> "Find the source of the glitch";
            case STAGE_REACHED_GLITCH_AREA -> "Defeat Glitch";
            case STAGE_DEFEATED_GLITCH -> "Return to the Real World";
            default -> "TO BE CONTINUED";
        };
    }
}
