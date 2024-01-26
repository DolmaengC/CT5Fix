package LCE;

public class LCS {

    /**
     * Get Longest common subsequence of Inter Arrays
     * @param target
     * @param tester
     * @return sb
     */
    public int[] LongestCommonSubsequenceofIntegerArray(int[] target, int[] tester) {
        // 1. get length of the subsequence with general bottom-up LCS algorithm
        int i = 0;
        int[][] dp = new int[target.length + 1][tester.length + 1];
        for (i = 1; i <= target.length; i++) {
            for (int j = 1; j <= tester.length; j++) {
                if (target[i - 1] == tester[j - 1]) {
                    dp[i][j] = dp[i - 1][j - 1] + 1;
                } else {
                    dp[i][j] = Math.max(dp[i - 1][j], dp[i][j - 1]);
                }
            }
        }
        // 2. get subseqeunce of two integer array
        int x = target.length;
        int y = tester.length;
        int[] sb = new int[dp[target.length][tester.length]]; // sub seqeunce
        int index = 0;
        while (x != 0 && y != 0) {
            if (target[x - 1] == tester[y - 1]) {
                sb[index] = target[x - 1];
                x--;
                y--;
                index++;
            } else if (dp[x][y] == dp[x - 1][y]) {
                x--;
            } else {
                y--;
            }
        }
        return sb; // sub seqeunce
    }

    public int[] Backtrack(int[] dp) {
        // reverse the dp array
        int[] sb = new int[dp.length];
        int index = 0;
        for (int i = dp.length - 1; i >= 0; i--) {
            sb[index] = dp[i];
            index++;
        }
        return sb;
    }

    public float ScoreSimilarity(int[] target, int[] tester) {
        float score = 1;
        int sum = 0;
        int[] dp = Backtrack(LongestCommonSubsequenceofIntegerArray(target, tester));
        int k_max = tester.length;
        int k = dp.length;
        int[] sigma = new int[k + 1];
        int trigger = 0;
        for (int i = 0; i < dp.length; i++) {
            sigma[i] = 0;
            for (int j = trigger; j < target.length; j++) {
                if (target[j] != dp[i]) {
                    sigma[i]++;
                } else {
                    trigger = j + 1;
                    break;
                }
            }
            if (i + 1 == dp.length) {
                sigma[i + 1] = target.length - trigger;
            }
        }
        for (int i = 1; i < sigma.length - 1; i++) {
            sum += sigma[i];
        }
        if (target.length != k)
            score = 1 - (float) sum / (target.length - k);
        score = score * ((float) k / k_max);
        return score;
    }
}
