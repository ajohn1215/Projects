#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <assert.h>

#define MAX_LENGTH 20
#ifndef MAX_SEQUENCE_CAP
#define MAX_SEQUENCE_CAP 50000
#endif

// Global game state arrays
int top_key[MAX_LENGTH];
int bottom_key[MAX_LENGTH];
int left_key[MAX_LENGTH];
int right_key[MAX_LENGTH];
char board[MAX_LENGTH][MAX_LENGTH];

// Solver constraint: for each board cell, tracks which digits (1..size) are still possible.
static bool solver_constraints[MAX_LENGTH][MAX_LENGTH][MAX_LENGTH];

// -----------------------
// Basic Utility Functions
// -----------------------

// Counts the number of "visible" buildings from one end of a line.
static int count_visible(const char line[], int size) {
    int visible = 0, tallest = 0;
    for (int i = 0; i < size; i++) {
        if (line[i] != '-') {
            int height = line[i] - '0';
            if (height > tallest) {
                tallest = height;
                visible++;
            }
        }
    }
    return visible;
}

// Prints the board state along with the clues.
void print_board_state(int size) {
    // Print top clues
    printf("   ");
    for (int j = 0; j < size; j++) {
        printf("%d ", top_key[j]);
    }
    printf("\n");
    
    // Print each row with left and right clues
    for (int i = 0; i < size; i++) {
        printf("%d  ", left_key[i]);
        for (int j = 0; j < size; j++) {
            printf("%c ", board[i][j]);
        }
        printf("%d\n", right_key[i]);
    }
    
    // Print bottom clues
    printf("   ");
    for (int j = 0; j < size; j++) {
        printf("%d ", bottom_key[j]);
    }
    printf("\n");
}

// Initializes the board and clue arrays from the given strings.
// The initial_state string should have size*size characters (row–major order),
// and the keys string must have 4*size characters ordered as top, bottom, left, right.
int initialize_board(const char *initial_state, const char *keys, int size) {
    // Fill board from initial_state
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            board[i][j] = initial_state[i * size + j];
        }
    }
    // Set clue arrays
    for (int i = 0; i < size; i++) {
        top_key[i]    = keys[i] - '0';
        bottom_key[i] = keys[size + i] - '0';
        left_key[i]   = keys[2 * size + i] - '0';
        right_key[i]  = keys[3 * size + i] - '0';
    }
    // Validate rows: no duplicate numbers in any row.
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            char curr = board[i][j];
            if (curr != '-') {
                for (int k = j + 1; k < size; k++) {
                    if (board[i][k] == curr)
                        return 0;
                }
            }
        }
    }
    // Validate columns: no duplicate numbers in any column.
    for (int j = 0; j < size; j++) {
        for (int i = 0; i < size; i++) {
            char curr = board[i][j];
            if (curr != '-') {
                for (int k = i + 1; k < size; k++) {
                    if (board[k][j] == curr)
                        return 0;
                }
            }
        }
    }
    // Validate complete rows against the clues.
    for (int i = 0; i < size; i++) {
        bool complete = true;
        char rowLine[MAX_LENGTH];
        for (int j = 0; j < size; j++) {
            rowLine[j] = board[i][j];
            if (board[i][j] == '-')
                complete = false;
        }
        if (complete) {
            int visL = count_visible(rowLine, size);
            if (left_key[i] != 0 && visL != left_key[i])
                return 0;
            char revRow[MAX_LENGTH];
            for (int j = 0; j < size; j++)
                revRow[j] = rowLine[size - 1 - j];
            int visR = count_visible(revRow, size);
            if (right_key[i] != 0 && visR != right_key[i])
                return 0;
        }
    }
    // Validate complete columns against the clues.
    for (int j = 0; j < size; j++) {
        bool complete = true;
        char colLine[MAX_LENGTH];
        for (int i = 0; i < size; i++) {
            colLine[i] = board[i][j];
            if (board[i][j] == '-')
                complete = false;
        }
        if (complete) {
            int visT = count_visible(colLine, size);
            if (top_key[j] != 0 && visT != top_key[j])
                return 0;
            char revCol[MAX_LENGTH];
            for (int i = 0; i < size; i++)
                revCol[i] = colLine[size - 1 - i];
            int visB = count_visible(revCol, size);
            if (bottom_key[j] != 0 && visB != bottom_key[j])
                return 0;
        }
    }
    return 1;
}

// -----------------------
// Solver Helper Functions
// -----------------------

// Attempt to place a piece at (row, col) and validate the move.
// (This is used during interactive moves in the original game but is also useful for testing.)
static int try_move(char piece, int row, int col, int size) {
    if (board[row][col] != '-') {
        printf("Invalid move. That space is already occupied.\n");
        return 0;
    }
    for (int j = 0; j < size; j++) {
        if (board[row][j] == piece) {
            printf("Invalid move. Number already exists in row.\n");
            return 0;
        }
    }
    for (int i = 0; i < size; i++) {
        if (board[i][col] == piece) {
            printf("Invalid move. Number already exists in column.\n");
            return 0;
        }
    }
    board[row][col] = piece;
    // Optionally, check if completed row/column satisfies the clues.
    bool rowComplete = true;
    char tempRow[MAX_LENGTH];
    for (int j = 0; j < size; j++) {
        tempRow[j] = board[row][j];
        if (board[row][j] == '-')
            rowComplete = false;
    }
    if (rowComplete) {
        int visL = count_visible(tempRow, size);
        if (left_key[row] != 0 && visL != left_key[row]) {
            board[row][col] = '-';
            printf("Invalid move. Violates left clue.\n");
            return 0;
        }
        char revRow[MAX_LENGTH];
        for (int j = 0; j < size; j++)
            revRow[j] = tempRow[size - 1 - j];
        int visR = count_visible(revRow, size);
        if (right_key[row] != 0 && visR != right_key[row]) {
            board[row][col] = '-';
            printf("Invalid move. Violates right clue.\n");
            return 0;
        }
    }
    bool colComplete = true;
    char tempCol[MAX_LENGTH];
    for (int i = 0; i < size; i++) {
        tempCol[i] = board[i][col];
        if (board[i][col] == '-')
            colComplete = false;
    }
    if (colComplete) {
        int visT = count_visible(tempCol, size);
        if (top_key[col] != 0 && visT != top_key[col]) {
            board[row][col] = '-';
            printf("Invalid move. Violates top clue.\n");
            return 0;
        }
        char revCol[MAX_LENGTH];
        for (int i = 0; i < size; i++)
            revCol[i] = tempCol[size - 1 - i];
        int visB = count_visible(revCol, size);
        if (bottom_key[col] != 0 && visB != bottom_key[col]) {
            board[row][col] = '-';
            printf("Invalid move. Violates bottom clue.\n");
            return 0;
        }
    }
    return 1;
}

// Recursively generate all valid sequences (permutations) for a given line (row or column)
// that satisfy the clue requirements. Valid sequences are stored in valid_seq.
static void generate_sequences_line(
    int idx, bool horizontal, int pos, bool used[], int curseq[],
    int **valid_seq, int *count_ptr, int size
) {
    if (pos == size) {
        int clue1 = (horizontal ? left_key[idx] : top_key[idx]);
        int clue2 = (horizontal ? right_key[idx] : bottom_key[idx]);
        int vis1 = 0, max1 = 0;
        for (int i = 0; i < size; i++) {
            if (curseq[i] > max1) { max1 = curseq[i]; vis1++; }
        }
        int vis2 = 0, max2 = 0;
        for (int i = size - 1; i >= 0; i--) {
            if (curseq[i] > max2) { max2 = curseq[i]; vis2++; }
        }
        if ((clue1 == 0 || vis1 == clue1) && (clue2 == 0 || vis2 == clue2)) {
            if (*count_ptr < MAX_SEQUENCE_CAP) {
                int *seq_copy = malloc(size * sizeof(int));
                for (int i = 0; i < size; i++) {
                    seq_copy[i] = curseq[i];
                }
                valid_seq[*count_ptr] = seq_copy;
                (*count_ptr)++;
            }
        }
        return;
    }
    int r, c;
    if (horizontal) { r = idx; c = pos; }
    else { r = pos; c = idx; }
    
    int fixed = 0;
    if (board[r][c] != '-') fixed = board[r][c] - '0';
    if (fixed) {
        if (!solver_constraints[r][c][fixed - 1])
            return;
        if (used[fixed])
            return;
        used[fixed] = true;
        curseq[pos] = fixed;
        generate_sequences_line(idx, horizontal, pos + 1, used, curseq, valid_seq, count_ptr, size);
        used[fixed] = false;
    } else {
        for (int d = 1; d <= size; d++) {
            if (!used[d] && solver_constraints[r][c][d - 1]) {
                used[d] = true;
                curseq[pos] = d;
                generate_sequences_line(idx, horizontal, pos + 1, used, curseq, valid_seq, count_ptr, size);
                used[d] = false;
            }
        }
    }
}

// Returns an array of valid sequences for a given line (row or column).
static int get_valid_sequences(int idx, bool horizontal, int ***seq_array_ptr, int size) {
    int **valid_seq = malloc(MAX_SEQUENCE_CAP * sizeof(int *));
    if (!valid_seq) return 0;
    int count = 0;
    bool used[MAX_LENGTH + 1] = { false };
    int curseq[MAX_LENGTH] = { 0 };
    generate_sequences_line(idx, horizontal, 0, used, curseq, valid_seq, &count, size);
    *seq_array_ptr = valid_seq;
    return count;
}

// Uses the valid sequences for a given line to refine the possibilities for each cell.
static bool sequence_filtration_line(bool horizontal, int idx, int size) {
    int **valid_seq;
    int seq_count = get_valid_sequences(idx, horizontal, &valid_seq, size);
    if (seq_count == 0) {
        free(valid_seq);
        return false;
    }
    bool changed = false;
    for (int pos = 0; pos < size; pos++) {
        int r = (horizontal ? idx : pos);
        int c = (horizontal ? pos : idx);
        if (board[r][c] != '-')
            continue;
        bool union_possible[MAX_LENGTH + 1] = { false };
        for (int s = 0; s < seq_count; s++) {
            int val = valid_seq[s][pos];
            union_possible[val] = true;
        }
        for (int d = 1; d <= size; d++) {
            if (solver_constraints[r][c][d - 1] && !union_possible[d]) {
                solver_constraints[r][c][d - 1] = false;
                changed = true;
            }
        }
        int cand_count = 0, last_d = -1;
        for (int d = 1; d <= size; d++) {
            if (solver_constraints[r][c][d - 1]) {
                cand_count++;
                last_d = d;
            }
        }
        if (cand_count == 1) {
            board[r][c] = '0' + last_d;
            changed = true;
        }
    }
    for (int s = 0; s < seq_count; s++) {
        free(valid_seq[s]);
    }
    free(valid_seq);
    return changed;
}

// Sorts the indices (rows or columns) in order of increasing number of empty cells.
static void get_sorted_indices(bool horizontal, int size, int sorted_indices[]) {
    for (int i = 0; i < size; i++) {
        sorted_indices[i] = i;
    }
    for (int i = 0; i < size - 1; i++) {
        for (int j = i + 1; j < size; j++) {
            int count_i = 0, count_j = 0;
            if (horizontal) {
                for (int k = 0; k < size; k++) {
                    if (board[sorted_indices[i]][k] == '-') count_i++;
                    if (board[sorted_indices[j]][k] == '-') count_j++;
                }
            } else {
                for (int k = 0; k < size; k++) {
                    if (board[k][sorted_indices[i]] == '-') count_i++;
                    if (board[k][sorted_indices[j]] == '-') count_j++;
                }
            }
            if (count_j < count_i) {
                int temp = sorted_indices[i];
                sorted_indices[i] = sorted_indices[j];
                sorted_indices[j] = temp;
            }
        }
    }
}

// Applies sequence filtration on all rows and columns until no further changes occur.
static bool apply_sequence_filtration_sorted(int size) {
    bool updated = false;
    int sorted_rows[MAX_LENGTH];
    get_sorted_indices(true, size, sorted_rows);
    for (int k = 0; k < size; k++) {
        int i = sorted_rows[k];
        if (sequence_filtration_line(true, i, size))
            updated = true;
    }
    int sorted_cols[MAX_LENGTH];
    get_sorted_indices(false, size, sorted_cols);
    for (int k = 0; k < size; k++) {
        int j = sorted_cols[k];
        if (sequence_filtration_line(false, j, size))
            updated = true;
    }
    return updated;
}

// -----------------------
// The Solver Function
// -----------------------

// Applies constraint propagation to refine the board state and prints the result.
int solve(const char *initial_state, const char *keys, int size) {
    if (!initialize_board(initial_state, keys, size)) {
        printf("Invalid initial board state.\n");
        return 0;
    }
    
    // Initialize solver constraints.
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            if (board[i][j] == '-') {
                for (int d = 0; d < size; d++)
                    solver_constraints[i][j][d] = true;
            } else {
                int val = board[i][j] - '0';
                for (int d = 0; d < size; d++)
                    solver_constraints[i][j][d] = (d == (val - 1));
            }
        }
    }
    
    // Apply initial constraints based on the clues.
    for (int j = 0; j < size; j++) {
        if (top_key[j] > 1 && top_key[j] < size) {
            int c = top_key[j];
            for (int d = 0; d < size; d++) {
                if (board[d][j] == '-') {
                    int lower_bound = size - c + 2 + d;
                    for (int v = lower_bound; v <= size; v++)
                        solver_constraints[d][j][v - 1] = false;
                }
            }
        }
        if (bottom_key[j] > 1 && bottom_key[j] < size) {
            int c = bottom_key[j];
            for (int d = 0; d < size; d++) {
                int row = size - 1 - d;
                if (board[row][j] == '-') {
                    int lower_bound = size - c + 2 + d;
                    for (int v = lower_bound; v <= size; v++)
                        solver_constraints[row][j][v - 1] = false;
                }
            }
        }
    }
    for (int i = 0; i < size; i++) {
        if (left_key[i] > 1 && left_key[i] < size) {
            int c = left_key[i];
            for (int d = 0; d < size; d++) {
                if (board[i][d] == '-') {
                    int lower_bound = size - c + 2 + d;
                    for (int v = lower_bound; v <= size; v++)
                        solver_constraints[i][d][v - 1] = false;
                }
            }
        }
        if (right_key[i] > 1 && right_key[i] < size) {
            int c = right_key[i];
            for (int d = 0; d < size; d++) {
                int col = size - 1 - d;
                if (board[i][col] == '-') {
                    int lower_bound = size - c + 2 + d;
                    for (int v = lower_bound; v <= size; v++)
                        solver_constraints[i][col][v - 1] = false;
                }
            }
        }
    }
    
    bool updated;
    do {
        updated = apply_sequence_filtration_sorted(size);
    } while (updated);
    
    print_board_state(size);
    return 1;
}

// -----------------------
// Main Entry Point for the Solver
// -----------------------

int main(int argc, char **argv) {
    if (argc != 4) {
        printf("Usage: %s size initial_state keys\n", argv[0]);
        return 1;
    }
    int size = (int)strtoul(argv[1], NULL, 10);
    if (solve(argv[2], argv[3], size))
        printf("Puzzle solved (if a valid solution exists) above.\n");
    else
        printf("Failed to solve puzzle.\n");
    return 0;
}
