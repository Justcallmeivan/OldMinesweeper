package com.example.ivan.minesweeper;

import android.util.Log;

import java.lang.Math;

public class GameLogic {

    int[][] mines;
    int[][] board;

    boolean firstTapLuck = true;
    boolean firstTap = true;

    int numMines;


    static final int EMPTY = 0;
    static final int ONE = 1;
    static final int TWO = 2;
    static final int THREE = 3;
    static final int FOUR = 4;
    static final int FIVE = 5;
    static final int SIX = 6;
    static final int SEVEN = 7;
    static final int EIGHT = 8;

    static final int UNTOUCHED = 10;
    static final int FLAG = 11;
    static final int QUESTION = 12;
    static final int MINE = 13;

    GameLogic(int row, int col, int numMines) {

        mines = new int[row][col];
        board = new int[row][col];
        this.numMines = numMines;
        setBoard();

//        setMines(row, col, numMines);
//        setBoard(row, col);

    }

    public void setMines() {
        int x;
        int y;
        for (y = 0; y < mines.length; y++) {
            for (x = 0; x < mines[0].length; x++) {
                mines[y][x] = EMPTY;
            }
        }
        for (int a = 0; a < numMines; ) {
            x = (int) (mines[0].length * Math.random());
            y = (int) (mines.length * Math.random());
            if (mines[y][x] == EMPTY) {
                mines[y][x] = MINE;
                a++;
            }
        }
        setNumbers();

    }
    public void setMines(int x, int y) {
        int a;
        int b;
        for (b = 0; b < mines.length; b++) {
            for (a = 0; a < mines[0].length; a++) {
                mines[b][a] = EMPTY;
            }
        }
        for (int c = 0; c < numMines; ) {
            a = (int) (mines[0].length * Math.random());
            b = (int) (mines.length * Math.random());
            if (mines[b][a] == EMPTY && ((a > x + 1 || a < x - 1) || (b > y + 1 || b < y - 1))) {
                mines[b][a] = MINE;
                c++;
            }
        }
        setNumbers();

    }

    public void setNumbers() {
        int num = 0;
        for (int y = 0; y < mines.length; y++) {
            for (int x = 0; x < mines[0].length; x++) {
                if (mines[y][x] == EMPTY) {
                    if (x - 1 >= 0) {
                        if (y - 1 >= 0 && mines[y - 1][x - 1] == MINE) {
                            num++;
                        }
                        if (mines[y][x - 1] == MINE) {
                            num++;
                        }
                        if (y + 1 < mines.length&& mines[y + 1][x - 1] == MINE) {
                            num++;
                        }
                    }
                    if (y - 1 >= 0 && mines[y - 1][x] == MINE) {
                        num++;
                    }
                    if (y + 1 < mines.length && mines[y + 1][x] == MINE) {
                        num++;
                    }
                    if (x + 1 < mines[0].length) {
                        if (y - 1 >= 0 && mines[y - 1][x + 1] == MINE) {
                            num++;
                        }
                        if (mines[y][x + 1] == MINE) {
                            num++;
                        }
                        if (y + 1 < mines.length && mines[y + 1][x + 1] == MINE) {
                            num++;
                        }
                    }
                    mines[y][x] = num;
                    num = 0;
                }

            }
        }
    }

    public void setBoard() {
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                board[y][x] = UNTOUCHED;
            }
        }
    }

    public void setFirstTapLuck(boolean bool){
        firstTapLuck = bool;
    }

    public void singleTap(int id){
        int x = convertId(id)[1];
        int y = convertId(id)[0];
        if(firstTap){
            if(firstTapLuck){
                setMines(x, y);
            }else{
                setMines();
            }
            firstTap = false;
        }

        if(board[y][x] != FLAG && board[y][x] != QUESTION) {
            board[y][x] = mines[y][x];
            openEmpty();
        }
        Log.d("Tap", "single G");
    }

    public void doubleTap(int id){
        int x = convertId(id)[1];
        int y = convertId(id)[0];
        Log.d("NumFlags", "" + numFlags(x, y));
        if(numFlags(x, y) == board[y][x] && noQuestions(x, y)){
            clearSurroundings(x, y);
        }
        openEmpty();
        Log.d("Tap", "double G");
    }

    public void hold(int id){
        int x = convertId(id)[1];
        int y = convertId(id)[0];
        if(board[y][x] == UNTOUCHED){
            board[y][x] = FLAG;
        }else if (board[y][x] == FLAG){
            board[y][x] = QUESTION;
        }else if (board[y][x] == QUESTION){
            board[y][x] = UNTOUCHED;
        }
        Log.d("Tap", "hold G");
    }

    public int[] convertId(int id){

        int x = id % board[0].length;
        int y = id / board[0].length ;

        return new int[]{y ,x};
    }

    public int numFlags(int x, int y){
        int num = 0;
        if (x - 1 >= 0) {
            if (y - 1 >= 0 && board[y - 1][x - 1] == FLAG) {
                num++;
            }
            if (board[y][x - 1] == FLAG) {
                num++;
            }
            if (y + 1 < board.length && board[y + 1][x - 1] == FLAG) {
                num++;
            }
        }
        if (y - 1 >= 0 && board[y - 1][x] == FLAG) {
            num++;
        }
        if (y + 1 < board.length && board[y + 1][x] == FLAG) {
            num++;
        }
        if (x + 1 < board[0].length) {
            if (y - 1 >= 0 && board[y - 1][x + 1] == FLAG) {
                num++;
            }
            if (board[y][x + 1] == FLAG) {
                num++;
            }
            if (y + 1 < board.length && board[y + 1][x + 1] == FLAG) {
                num++;
            }
        }
        return num;
    }

    public boolean noQuestions(int x, int y){
        if (x - 1 >= 0) {
            if (y - 1 >= 0 && mines[y - 1][x - 1] == QUESTION) {
                return false;
            }
            if (mines[y][x - 1] == QUESTION) {
                return false;
            }
            if (y + 1 < mines.length && mines[y + 1][x - 1] == QUESTION) {
                return false;
            }
        }
        if (y - 1 >= 0 && mines[y - 1][x] == QUESTION) {
            return false;
        }
        if (y + 1 < mines.length && mines[y + 1][x] == QUESTION) {
            return false;
        }
        if (x + 1 < mines[0].length) {
            if (y - 1 >= 0 && mines[y - 1][x + 1] == QUESTION) {
                return false;
            }
            if (mines[y][x + 1] == QUESTION) {
                return false;
            }
            if (y + 1 < mines.length && mines[y + 1][x + 1] == QUESTION) {
                return false;
            }
        }
        return true;

    }

    public void clearSurroundings(int x, int y){
        if (x - 1 >= 0) {
            if (y - 1 >= 0 && board[y - 1][x - 1] != FLAG) {
                board[y-1][x-1] = mines[y-1][x-1];
            }
            if (board[y][x - 1] != FLAG) {
                board[y][x-1] = mines[y][x-1];
            }
            if (y + 1 < board.length && board[y + 1][x - 1] != FLAG) {
                board[y+1][x-1] = mines[y+1][x-1];
            }
        }
        if (y - 1 >= 0 && board[y - 1][x] != FLAG) {
            board[y-1][x] = mines[y-1][x];
        }
        if (y + 1 < board.length && board[y + 1][x] != FLAG) {
            board[y+1][x] = mines[y+1][x];
        }
        if (x + 1 < board[0].length) {
            if (y - 1 >= 0 && board[y - 1][x + 1] != FLAG) {
                board[y-1][x+1] = mines[y-1][x+1];
            }
            if (board[y][x + 1] != FLAG) {
                board[y][x+1] = mines[y][x+1];
            }
            if (y + 1 < board.length && board[y + 1][x + 1] != FLAG) {
                board[y+1][x+1] = mines[y+1][x+1];
            }
        }
    }

    public void openEmpty(){
        int numEmpty = 0;
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                if(board[y][x] == EMPTY){
                   numEmpty++;
                }
            }
        }
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                if(board[y][x] == EMPTY){
                    clearSurroundings(x, y);
                }
            }
        }
        int newNumEmpty = 0;
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                if(board[y][x] == EMPTY){
                    newNumEmpty++;
                }
            }
        }
        Log.d("Empty", "old " + numEmpty);
        Log.d("Empty", "new " + newNumEmpty);

        if(newNumEmpty > numEmpty){
            openEmpty();
        }

    }

    public int[] getConvertedBoard(){
        int[] convertedBoard = new int[board.length * board[0].length];
        for (int y = 0; y < board.length; y++) {
            for (int x = 0; x < board[0].length; x++) {
                convertedBoard[x + y * board[0].length] = board[y][x];
            }
        }
        return convertedBoard;
    }
}
