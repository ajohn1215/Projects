#include <stdio.h>
#include <stdlib.h>
#include <stdbool.h>
#include <string.h>
#include <assert.h>
#include <SDL2/SDL.h>
#include <SDL2/SDL_ttf.h>

#define MAX_LENGTH 20      // Maximum board dimension
#define CELL_SIZE 50       // Pixel size of each board cell
#define MARGIN 50          // Margin around the board

// Global game state arrays
int top_key[MAX_LENGTH];
int bottom_key[MAX_LENGTH];
int left_key[MAX_LENGTH];
int right_key[MAX_LENGTH];
char board[MAX_LENGTH][MAX_LENGTH];

//--------------------
// Game Logic Functions
//--------------------

// Returns the number of "visible" buildings in a line.
int count_visible(const char line[], int size) {
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

// Initializes the board using an initial state string and a keys string.
// The keys string must have 4*size characters (top, bottom, left, right).
int initialize_board(const char *initial_state, const char *keys, int size) {
    // Fill board from initial_state (row-major order)
    for (int i = 0; i < size; i++)
        for (int j = 0; j < size; j++)
            board[i][j] = initial_state[i * size + j];
    
    // Set clues (keys)
    for (int i = 0; i < size; i++) {
        top_key[i]    = keys[i] - '0';
        bottom_key[i] = keys[size + i] - '0';
        left_key[i]   = keys[2 * size + i] - '0';
        right_key[i]  = keys[3 * size + i] - '0';
    }
    
    // Validate no duplicate numbers in any row
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            char curr = board[i][j];
            if (curr != '-') {
                for (int k = j + 1; k < size; k++)
                    if (board[i][k] == curr)
                        return 0;
            }
        }
    }
    
    // Validate no duplicate numbers in any column
    for (int col = 0; col < size; col++) {
        for (int row = 0; row < size; row++) {
            char curr = board[row][col];
            if (curr != '-') {
                for (int k = row + 1; k < size; k++)
                    if (board[k][col] == curr)
                        return 0;
            }
        }
    }
    
    // Validate completed rows against the clues
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
    
    // Validate completed columns against the clues
    for (int col = 0; col < size; col++) {
        bool complete = true;
        char colLine[MAX_LENGTH];
        for (int i = 0; i < size; i++) {
            colLine[i] = board[i][col];
            if (board[i][col] == '-')
                complete = false;
        }
        if (complete) {
            int visT = count_visible(colLine, size);
            if (top_key[col] != 0 && visT != top_key[col])
                return 0;
            char revCol[MAX_LENGTH];
            for (int i = 0; i < size; i++)
                revCol[i] = colLine[size - 1 - i];
            int visB = count_visible(revCol, size);
            if (bottom_key[col] != 0 && visB != bottom_key[col])
                return 0;
        }
    }
    return 1;
}

// Attempts to place 'piece' at the specified (row, col).
// Returns 1 if successful, or 0 if the move is invalid.
int try_move(char piece, int row, int col, int size) {
    if (board[row][col] != '-') {
        printf("Invalid move. That space is already occupied.\n");
        return 0;
    }
    // Check row for duplicate number
    for (int j = 0; j < size; j++)
        if (board[row][j] == piece) {
            printf("Invalid move. Number already exists in this row.\n");
            return 0;
        }
    // Check column for duplicate number
    for (int i = 0; i < size; i++)
        if (board[i][col] == piece) {
            printf("Invalid move. Number already exists in this column.\n");
            return 0;
        }
    
    board[row][col] = piece;
    return 1;
}

// Checks if the board is completely filled.
bool is_board_full(int size) {
    for (int i = 0; i < size; i++)
        for (int j = 0; j < size; j++)
            if (board[i][j] == '-')
                return false;
    return true;
}

//--------------------
// SDL2 Rendering Functions
//--------------------

// Renders the given text at (x, y) using the provided font.
void render_text(SDL_Renderer *renderer, TTF_Font *font, const char *text, int x, int y) {
    SDL_Color color = {0, 0, 0, 255}; // Black text
    SDL_Surface *surface = TTF_RenderText_Blended(font, text, color);
    if (!surface) return;
    SDL_Texture *texture = SDL_CreateTextureFromSurface(renderer, surface);
    SDL_Rect dst = { x, y, surface->w, surface->h };
    SDL_RenderCopy(renderer, texture, NULL, &dst);
    SDL_DestroyTexture(texture);
    SDL_FreeSurface(surface);
}

// Global variables to track the currently selected cell.
int selectedRow = -1, selectedCol = -1;

// Global error message and its timestamp.
char errorMessage[256] = "";
Uint32 errorMessageTime = 0;
const Uint32 ERROR_DURATION = 2000; // Display error for 2 seconds

// Renders the board, numbers, clues, and selection highlight.
void render_board(SDL_Renderer *renderer, TTF_Font *font, int size) {
    int boardPixelSize = size * CELL_SIZE;
    int startX = MARGIN;
    int startY = MARGIN;
    
    // Draw board background
    SDL_SetRenderDrawColor(renderer, 255, 255, 255, 255); // White
    SDL_Rect boardRect = { startX, startY, boardPixelSize, boardPixelSize };
    SDL_RenderFillRect(renderer, &boardRect);
    
    // Draw grid lines
    SDL_SetRenderDrawColor(renderer, 0, 0, 0, 255); // Black
    for (int i = 0; i <= size; i++) {
        // Horizontal line
        SDL_RenderDrawLine(renderer, startX, startY + i * CELL_SIZE, startX + boardPixelSize, startY + i * CELL_SIZE);
        // Vertical line
        SDL_RenderDrawLine(renderer, startX + i * CELL_SIZE, startY, startX + i * CELL_SIZE, startY + boardPixelSize);
    }
    
    // Render numbers inside cells
    for (int i = 0; i < size; i++) {
        for (int j = 0; j < size; j++) {
            if (board[i][j] != '-') {
                char text[2] = { board[i][j], '\0' };
                // Center the text in the cell (adjust offsets as needed)
                int textX = startX + j * CELL_SIZE + CELL_SIZE / 2 - 5;
                int textY = startY + i * CELL_SIZE + CELL_SIZE / 2 - 10;
                render_text(renderer, font, text, textX, textY);
            }
        }
    }
    
    // Highlight the selected cell
    if (selectedRow >= 0 && selectedCol >= 0) {
        SDL_Rect selRect = { startX + selectedCol * CELL_SIZE, startY + selectedRow * CELL_SIZE, CELL_SIZE, CELL_SIZE };
        SDL_SetRenderDrawColor(renderer, 255, 0, 0, 255); // Red highlight
        SDL_RenderDrawRect(renderer, &selRect);
        SDL_RenderDrawRect(renderer, &selRect);
    }
    
    // Render clues around the board
    char clueText[4];
    // Top clues
    for (int j = 0; j < size; j++) {
        sprintf(clueText, "%d", top_key[j]);
        int textX = startX + j * CELL_SIZE + CELL_SIZE/2 - 5;
        int textY = startY - 30;
        render_text(renderer, font, clueText, textX, textY);
    }
    // Bottom clues
    for (int j = 0; j < size; j++) {
        sprintf(clueText, "%d", bottom_key[j]);
        int textX = startX + j * CELL_SIZE + CELL_SIZE/2 - 5;
        int textY = startY + boardPixelSize + 10;
        render_text(renderer, font, clueText, textX, textY);
    }
    // Left clues
    for (int i = 0; i < size; i++) {
        sprintf(clueText, "%d", left_key[i]);
        int textX = startX - 30;
        int textY = startY + i * CELL_SIZE + CELL_SIZE/2 - 10;
        render_text(renderer, font, clueText, textX, textY);
    }
    // Right clues
    for (int i = 0; i < size; i++) {
        sprintf(clueText, "%d", right_key[i]);
        int textX = startX + boardPixelSize + 10;
        int textY = startY + i * CELL_SIZE + CELL_SIZE/2 - 10;
        render_text(renderer, font, clueText, textX, textY);
    }
}

//--------------------
// Main SDL2 Game Loop
//--------------------
int main(int argc, char **argv) {
    int size;
    const char *initial_state;
    const char *keys;
    
    // Expect 3 command-line arguments: size, initial_state, and keys.
    if (argc == 4) {
        size = (int)strtoul(argv[1], NULL, 10);
        initial_state = argv[2];
        keys = argv[3];
    } else {
        // Default 4x4 puzzle if arguments are not provided.
        size = 4;
        initial_state = "----"  // row 0
                        "----"  // row 1
                        "----"  // row 2
                        "----"; // row 3
        // Default keys: top ("2134"), bottom ("3412"), left ("1234"), right ("4321")
        keys = "2134" "3412" "1234" "4321";
        printf("Usage: %s size initial_state keys\nUsing default 4x4 puzzle.\n", argv[0]);
    }
    
    if (size > MAX_LENGTH) {
        fprintf(stderr, "Board size exceeds maximum allowed (%d).\n", MAX_LENGTH);
        return 1;
    }
    
    if (!initialize_board(initial_state, keys, size)) {
        fprintf(stderr, "Invalid initial board state or keys.\n");
        return 1;
    }
    
    // Initialize SDL and SDL_ttf
    if (SDL_Init(SDL_INIT_VIDEO) < 0) {
        fprintf(stderr, "SDL could not initialize. Error: %s\n", SDL_GetError());
        return 1;
    }
    if (TTF_Init() < 0) {
        fprintf(stderr, "SDL_ttf could not initialize. Error: %s\n", TTF_GetError());
        SDL_Quit();
        return 1;
    }
    
    int windowWidth = size * CELL_SIZE + 2 * MARGIN;
    int windowHeight = size * CELL_SIZE + 2 * MARGIN;
    
    SDL_Window *window = SDL_CreateWindow("Skyscraper Puzzle Game",
                                          SDL_WINDOWPOS_CENTERED,
                                          SDL_WINDOWPOS_CENTERED,
                                          windowWidth, windowHeight,
                                          SDL_WINDOW_SHOWN);
    if (!window) {
        fprintf(stderr, "Window could not be created. Error: %s\n", SDL_GetError());
        TTF_Quit();
        SDL_Quit();
        return 1;
    }
    
    SDL_Renderer *renderer = SDL_CreateRenderer(window, -1, SDL_RENDERER_ACCELERATED);
    if (!renderer) {
        fprintf(stderr, "Renderer could not be created. Error: %s\n", SDL_GetError());
        SDL_DestroyWindow(window);
        TTF_Quit();
        SDL_Quit();
        return 1;
    }
    
    // Load a TTF font (ensure that "Arial.ttf" is available or change the filename)
    TTF_Font *font = TTF_OpenFont("Arial.ttf", 24);
    if (!font) {
        fprintf(stderr, "Failed to load font. Error: %s\n", TTF_GetError());
        SDL_DestroyRenderer(renderer);
        SDL_DestroyWindow(window);
        TTF_Quit();
        SDL_Quit();
        return 1;
    }
    
    bool running = true;
    SDL_Event event;
    
    while (running) {
        // Event handling
        while (SDL_PollEvent(&event)) {
            switch (event.type) {
                case SDL_QUIT:
                    running = false;
                    break;
                case SDL_MOUSEBUTTONDOWN:
                    if (event.button.button == SDL_BUTTON_LEFT) {
                        int x = event.button.x;
                        int y = event.button.y;
                        int boardX = MARGIN;
                        int boardY = MARGIN;
                        int boardPixelSize = size * CELL_SIZE;
                        // Check if click occurred within the board area
                        if (x >= boardX && x < boardX + boardPixelSize &&
                            y >= boardY && y < boardY + boardPixelSize) {
                            selectedCol = (x - boardX) / CELL_SIZE;
                            selectedRow = (y - boardY) / CELL_SIZE;
                        } else {
                            selectedRow = -1;
                            selectedCol = -1;
                        }
                    }
                    break;
                case SDL_KEYDOWN:
                    // If a cell is selected, check if a valid digit key is pressed.
                    if (selectedRow != -1 && selectedCol != -1) {
                        char keyChar = (char)event.key.keysym.sym;
                        if (keyChar >= '1' && keyChar <= ('0' + size)) {
                            if (try_move(keyChar, selectedRow, selectedCol, size)) {
                                errorMessage[0] = '\0';
                            } else {
                                snprintf(errorMessage, sizeof(errorMessage), "Invalid move at (%d, %d) with %c", selectedRow, selectedCol, keyChar);
                                errorMessageTime = SDL_GetTicks();
                            }
                        }
                    }
                    // Allow quitting with the 'q' key.
                    if (event.key.keysym.sym == SDLK_q)
                        running = false;
                    break;
                default:
                    break;
            }
        }
        
        // Clear the renderer with a light gray background.
        SDL_SetRenderDrawColor(renderer, 220, 220, 220, 255);
        SDL_RenderClear(renderer);
        
        // Render the board and clues.
        render_board(renderer, font, size);
        
        // Render an error message if one exists and hasn't expired.
        if (errorMessage[0] != '\0' && SDL_GetTicks() - errorMessageTime < ERROR_DURATION)
            render_text(renderer, font, errorMessage, MARGIN, windowHeight - MARGIN / 2);
        
        // If the board is completely filled, display a congratulations message.
        if (is_board_full(size))
            render_text(renderer, font, "Congratulations! Puzzle Completed.", MARGIN, windowHeight / 2 - 20);
        
        SDL_RenderPresent(renderer);
        SDL_Delay(16); // Approximately 60 FPS
    }
    
    // Cleanup resources.
    TTF_CloseFont(font);
    SDL_DestroyRenderer(renderer);
    SDL_DestroyWindow(window);
    TTF_Quit();
    SDL_Quit();
    
    return 0;
}
