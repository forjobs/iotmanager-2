package com.naumchevski.iotcontrol.util;

import com.naumchevski.iotcontrol.type.VoiceActionType;

/**
 * Created by Naumchevski on 2/28/2016.
 */
public final class VoiceCmdUtil {

    private VoiceCmdUtil() {}

    public static int getCounterIndexFromVoiceCmd(String spokenText) {
        String cmd = spokenText.toLowerCase();
        String[] list1 =
                new String[]{"one", "two", "three", "four", "five", "six", "seven", "eight", "nine", "ten"};
        String[] list2 =
                new String[]{"first", "second", "third", "fourth", "fifth", "sixth", "seventh", "eighth", "ninth", "tenth"};
        String[] list3 =
            new String[]{"1st", "2nd", "3rd", "4th", "5th", "6th", "7th", "8th", "9th", "10th"};

        for (int i = 0; i < list1.length; i++) {
            if (cmd.contains(list1[i])) {
                return i;
            }
        }
        for (int i = 0; i < list2.length; i++) {
            if (cmd.contains(list2[i])) {
                return i;
            }
        }
        for (int i = 0; i < list3.length; i++) {
            if (cmd.contains(list3[i])) {
                return i;
            }
        }

        return -1; // not found
    }

    public static VoiceActionType getCounterActionFromVoiceCmd(String spokenText) {
        String cmd = spokenText.toLowerCase();
        if (cmd.contains("turn on") || cmd.contains("switch on")) {
            return VoiceActionType.TURN_ON;
        } else if (cmd.contains("turn off") || cmd.contains("switch off") || cmd.contains("shut down")) {
            return VoiceActionType.TURN_OFF;
        }

        return VoiceActionType.NO_SUPPORTED;
    }
}
