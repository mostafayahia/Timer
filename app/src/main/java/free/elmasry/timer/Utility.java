/*
 * Copyright (C) 2018 Yahia H. El-Tayeb
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package free.elmasry.timer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;

import android.content.Context;
import android.os.Build;
import android.util.SparseIntArray;
import android.widget.Toast;

public class Utility {

    // sparseIntArray gives a better performance than hash map in this case
    static final SparseIntArray SURA_NUM_VERSES_NUM_MAP;

    static {
        SURA_NUM_VERSES_NUM_MAP = new SparseIntArray();
        SURA_NUM_VERSES_NUM_MAP.put(1, 7);
        SURA_NUM_VERSES_NUM_MAP.put(2, 286);
        SURA_NUM_VERSES_NUM_MAP.put(3, 200);
        SURA_NUM_VERSES_NUM_MAP.put(4, 176);
        SURA_NUM_VERSES_NUM_MAP.put(5, 120);
        SURA_NUM_VERSES_NUM_MAP.put(6, 165);
        SURA_NUM_VERSES_NUM_MAP.put(7, 206);
        SURA_NUM_VERSES_NUM_MAP.put(8, 75);
        SURA_NUM_VERSES_NUM_MAP.put(9, 129);
        SURA_NUM_VERSES_NUM_MAP.put(10, 109);
        SURA_NUM_VERSES_NUM_MAP.put(11, 123);
        SURA_NUM_VERSES_NUM_MAP.put(12, 111);
        SURA_NUM_VERSES_NUM_MAP.put(13, 43);
        SURA_NUM_VERSES_NUM_MAP.put(14, 52);
        SURA_NUM_VERSES_NUM_MAP.put(15, 99);
        SURA_NUM_VERSES_NUM_MAP.put(16, 128);
        SURA_NUM_VERSES_NUM_MAP.put(17, 111);
        SURA_NUM_VERSES_NUM_MAP.put(18, 110);
        SURA_NUM_VERSES_NUM_MAP.put(19, 98);
        SURA_NUM_VERSES_NUM_MAP.put(20, 135);

        SURA_NUM_VERSES_NUM_MAP.put(21, 112);
        SURA_NUM_VERSES_NUM_MAP.put(22, 87);
        SURA_NUM_VERSES_NUM_MAP.put(23, 118);
        SURA_NUM_VERSES_NUM_MAP.put(24, 64);
        SURA_NUM_VERSES_NUM_MAP.put(25, 77);
        SURA_NUM_VERSES_NUM_MAP.put(26, 227);
        SURA_NUM_VERSES_NUM_MAP.put(27, 93);
        SURA_NUM_VERSES_NUM_MAP.put(28, 88);
        SURA_NUM_VERSES_NUM_MAP.put(29, 69);
        SURA_NUM_VERSES_NUM_MAP.put(30, 60);
        SURA_NUM_VERSES_NUM_MAP.put(31, 34);
        SURA_NUM_VERSES_NUM_MAP.put(32, 30);
        SURA_NUM_VERSES_NUM_MAP.put(33, 73);

        SURA_NUM_VERSES_NUM_MAP.put(34, 54);
        SURA_NUM_VERSES_NUM_MAP.put(35, 45);
        SURA_NUM_VERSES_NUM_MAP.put(36, 83);
        SURA_NUM_VERSES_NUM_MAP.put(37, 182);
        SURA_NUM_VERSES_NUM_MAP.put(38, 88);
        SURA_NUM_VERSES_NUM_MAP.put(39, 75);
        SURA_NUM_VERSES_NUM_MAP.put(40, 85);
        SURA_NUM_VERSES_NUM_MAP.put(41, 54);
        SURA_NUM_VERSES_NUM_MAP.put(42, 53);
        SURA_NUM_VERSES_NUM_MAP.put(43, 89);
        SURA_NUM_VERSES_NUM_MAP.put(44, 59);
        SURA_NUM_VERSES_NUM_MAP.put(45, 37);

        SURA_NUM_VERSES_NUM_MAP.put(46, 35);
        SURA_NUM_VERSES_NUM_MAP.put(47, 38);
        SURA_NUM_VERSES_NUM_MAP.put(48, 29);
        SURA_NUM_VERSES_NUM_MAP.put(49, 18);
        SURA_NUM_VERSES_NUM_MAP.put(50, 45);
        SURA_NUM_VERSES_NUM_MAP.put(51, 60);
        SURA_NUM_VERSES_NUM_MAP.put(52, 49);
        SURA_NUM_VERSES_NUM_MAP.put(53, 62);
        SURA_NUM_VERSES_NUM_MAP.put(54, 55);
        SURA_NUM_VERSES_NUM_MAP.put(55, 78);
        SURA_NUM_VERSES_NUM_MAP.put(56, 96);
        SURA_NUM_VERSES_NUM_MAP.put(57, 29);

        SURA_NUM_VERSES_NUM_MAP.put(58, 22);
        SURA_NUM_VERSES_NUM_MAP.put(59, 24);
        SURA_NUM_VERSES_NUM_MAP.put(60, 13);
        SURA_NUM_VERSES_NUM_MAP.put(61, 14);
        SURA_NUM_VERSES_NUM_MAP.put(62, 11);
        SURA_NUM_VERSES_NUM_MAP.put(63, 11);
        SURA_NUM_VERSES_NUM_MAP.put(64, 18);
        SURA_NUM_VERSES_NUM_MAP.put(65, 12);
        SURA_NUM_VERSES_NUM_MAP.put(66, 12);

        SURA_NUM_VERSES_NUM_MAP.put(67, 30);
        SURA_NUM_VERSES_NUM_MAP.put(68, 52);
        SURA_NUM_VERSES_NUM_MAP.put(69, 52);
        SURA_NUM_VERSES_NUM_MAP.put(70, 44);
        SURA_NUM_VERSES_NUM_MAP.put(71, 28);
        SURA_NUM_VERSES_NUM_MAP.put(72, 28);
        SURA_NUM_VERSES_NUM_MAP.put(73, 20);
        SURA_NUM_VERSES_NUM_MAP.put(74, 56);
        SURA_NUM_VERSES_NUM_MAP.put(75, 40);
        SURA_NUM_VERSES_NUM_MAP.put(76, 31);
        SURA_NUM_VERSES_NUM_MAP.put(77, 50);

        SURA_NUM_VERSES_NUM_MAP.put(78, 40);
        SURA_NUM_VERSES_NUM_MAP.put(79, 46);
        SURA_NUM_VERSES_NUM_MAP.put(80, 42);
        SURA_NUM_VERSES_NUM_MAP.put(81, 29);
        SURA_NUM_VERSES_NUM_MAP.put(82, 19);
        SURA_NUM_VERSES_NUM_MAP.put(83, 36);
        SURA_NUM_VERSES_NUM_MAP.put(84, 25);
        SURA_NUM_VERSES_NUM_MAP.put(85, 22);
        SURA_NUM_VERSES_NUM_MAP.put(86, 17);
        SURA_NUM_VERSES_NUM_MAP.put(87, 19);
        SURA_NUM_VERSES_NUM_MAP.put(88, 26);
        SURA_NUM_VERSES_NUM_MAP.put(89, 30);
        SURA_NUM_VERSES_NUM_MAP.put(90, 20);
        SURA_NUM_VERSES_NUM_MAP.put(91, 15);
        SURA_NUM_VERSES_NUM_MAP.put(92, 21);
        SURA_NUM_VERSES_NUM_MAP.put(93, 11);
        SURA_NUM_VERSES_NUM_MAP.put(94, 8);
        SURA_NUM_VERSES_NUM_MAP.put(95, 8);
        SURA_NUM_VERSES_NUM_MAP.put(96, 19);
        SURA_NUM_VERSES_NUM_MAP.put(97, 5);
        SURA_NUM_VERSES_NUM_MAP.put(98, 8);
        SURA_NUM_VERSES_NUM_MAP.put(99, 8);
        SURA_NUM_VERSES_NUM_MAP.put(100, 11);
        SURA_NUM_VERSES_NUM_MAP.put(101, 11);
        SURA_NUM_VERSES_NUM_MAP.put(102, 8);
        SURA_NUM_VERSES_NUM_MAP.put(103, 3);
        SURA_NUM_VERSES_NUM_MAP.put(104, 9);
        SURA_NUM_VERSES_NUM_MAP.put(105, 5);
        SURA_NUM_VERSES_NUM_MAP.put(106, 4);
        SURA_NUM_VERSES_NUM_MAP.put(107, 7);
        SURA_NUM_VERSES_NUM_MAP.put(108, 3);
        SURA_NUM_VERSES_NUM_MAP.put(109, 6);
        SURA_NUM_VERSES_NUM_MAP.put(110, 3);
        SURA_NUM_VERSES_NUM_MAP.put(111, 5);
        SURA_NUM_VERSES_NUM_MAP.put(112, 4);
        SURA_NUM_VERSES_NUM_MAP.put(113, 5);
        SURA_NUM_VERSES_NUM_MAP.put(114, 6);
    }

    static boolean isFileExist(String path) {
        // test if the selected file is already on the android device
        File file = new File(path);
        if (file.exists())
            return true;
        return false;
    }

    /**
     * show message using toast
     *
     * @param resid the resource id of the message which you want to show
     */
    static void showToastMessage(Context context, int resid) {
        Toast.makeText(context, context.getString(resid), Toast.LENGTH_SHORT).show();
    }

    /**
     * show message using toast
     *
     * @param message the message which you want to show
     */
    static void showToastMessage(Context context, String message) {
        Toast.makeText(context, message, Toast.LENGTH_SHORT).show();
    }

    static boolean isLollipop() {

        int apiLevel = Build.VERSION.SDK_INT;

        // targeting lollipop and lollipop_mr1
        return apiLevel == 21 || apiLevel == 22;
    }

    static String getSuraName(Context context, int suraNo) {

        // you can use add_sura_spinner_list or choose_sura_spinner_list
        String[] allSuraNames = context.getResources().getStringArray(R.array.add_sura_spinner_list);

        // sura string in format like "1. البقرة"
        String suraNoAndName = allSuraNames[suraNo];

        int dotIndex = suraNoAndName.indexOf(".");

        return context.getString(R.string.label_sura) + " " +
                allSuraNames[suraNo].substring(dotIndex + 1).trim();
    }
}
