/*
 * Copyright (c) 2018. 18-12-10 下午8:14.
 * @author 李高丞
 */

package handler;

import mainGUI.Main;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import controller.Controller;
import controller.MainController;
import entity.ExtendedInfo;
import entity.Music;
import entity.Song;
import entity.Tag;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Alert;
import javafx.scene.control.TableView;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import service.MusicMediaPlayer;
import service.PlayState;
import tool.ExcelTool;
import tool.GetMusicInfo;
import tool.PDFTool;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Handler 类
 * 设计此类的目的主要就是为了减少各个控制器中的事件处理方法
 *
 * @author 李高丞
 * @version 1.0 Beta
 */
public class Handler {

    /**
     * 获取 Main 类
     */
    private static Main mainGUI;
    /**
     * 获取当前播放器播放状态
     */
    private static PlayState playState = PlayState.getPlayState();
    /**
     * 设置播放列表弹出状态为：不弹出
     */
    private boolean listOn = false;
    /**
     * 设置一个字符串属性
     */
    private static StringProperty message = new SimpleStringProperty();
    /**
     * 获取一个播放器
     */
    private MusicMediaPlayer player;
    /**
     * 判断是否为第一次生成 Excel
     */
    private static int isFirstExportExcel = 1;
    /**
     * 判断是否为第一次生成 PDF
     */
    private static int isFirstExportPDF = 1;


    /**
     * 获得 main类
     *
     * @param mainGUI 主类
     */
    public Handler(Main mainGUI) {
        Handler.mainGUI = mainGUI;
    }

    /**
     * 搜索事件
     */
    public void search() {
        // 获得主类控制器
        MainController mainController = mainGUI.getMainController();
        // 获得 TextField 里的文本
        String msg = mainController.getTextField_searchSong().getText();
        // 设置搜索按钮的事件处理
        mainController.getButton_search().setOnAction(new EventHandler<ActionEvent>() {
            @Override
            public void handle(ActionEvent event) {
                if (msg != null) {
                    // 获取网络输出流然后输出搜索信息
                    MainController.getServerOut().println("# search " + msg);
                    // 启动线程来接受文件
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            try {
                                // 接受文件
                                Handler.receiveFile(MainController.getSocket());
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }).start();

                    // 等待数据传输结束
                    try {
                        Thread.sleep(500);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });
    }

    /**
     * 导出 Excel 信息
     */
    public void exportExcel() {
        try {
            /*
             * 如果是第一次生成 Excel，不会生成 Excel 文件而是用一个对话框提示
             */
            if (isFirstExportExcel == 1){
                isFirstExportExcel++;
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("通知");
                a.setHeaderText("^-^");
                a.setContentText("请从我的音乐界面单击选择一首歌然后导出它的Excel信息");
                a.show();
                return;
            }
            Music music;

            // 获取被选中的音乐
            if ((music = mainGUI.
                    getMyMusicPageController().
                    getTableView_songList().
                    getSelectionModel().
                    getSelectedItem()) != null) {
                // 生成 Excel 文件
                ExcelTool.CreateMsg(music);
                // 提示信息
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("通知");
                alert.setHeaderText("^-^");
                alert.setContentText("输出文件已保存至 D:/" + music.getMusicTitle() + ".xls");
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("通知");
                alert.setHeaderText("^-^");
                alert.setContentText("请选中文件");
                alert.show();
            }
        } catch (Exception e) {
            System.err.println("Error in export Excel");
        }

    }

    /**
     * 导出 PDF 信息
     */
    public void exportPDF() {
        try {
            /*
             * 如果是第一次生成 PDF，不会生成 PDF 文件而是用一个对话框提示
             */
            if (isFirstExportPDF == 1){
                isFirstExportPDF++;
                Alert a = new Alert(Alert.AlertType.CONFIRMATION);
                a.setTitle("通知");
                a.setHeaderText("^-^");
                a.setContentText("请从我的音乐界面单击选择一首歌然后导出它的PDF信息");
                a.show();
                return;
            }

            Music music;

            // 获取被选中的音乐
            if ((music = mainGUI.
                    getMyMusicPageController().
                    getTableView_songList().
                    getSelectionModel().
                    getSelectedItem()) != null)
            {
                // 生成 PDF 文件
                PDFTool.CreatePDF(music);
                // 提示信息
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("通知");
                alert.setHeaderText("^-^");
                alert.setContentText("输出文件已保存至 D:/" + music.getMusicTitle() + ".pdf");
                alert.show();
            } else {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("通知");
                alert.setHeaderText("^-^");
                alert.setContentText("请选中文件");
                alert.show();
            }
        } catch (Exception e) {
            System.err.println("Error in export PDF");
        }
    }


    /**
     * 调用该方法就开始播放歌曲（鼠标双击 + 右键的播放）
     */
    public void play(Controller source) {

        // 获取在我的音乐界面被选择的歌曲
        Music index = source.getTableView_songList().getSelectionModel().getSelectedItem();

        // 获取播放列表的 TableView
        TableView<Music> tableView = mainGUI.getPlayListController().getTableView_songList();

        // 如果播放列表的 TableView 里没有这个歌曲，那就直接添加这个歌曲
        if (!tableView.getItems().contains(index))
            tableView.getItems().add(index);

        System.out.println(tableView.getItems());

        // 获取扩展信息
        ExtendedInfo info = GetMusicInfo.getExtendedInfo(index.getPath());

        playState.setCurrentMusic(index);
        // 开始播放歌曲
        player = new MusicMediaPlayer(index);
        player.start();

        // 将字符串协议和主界面的播放列表内歌曲数目绑定，
        // 每次播放都会更新
        message.set(String.valueOf(mainGUI.getPlayListController().getTableView_songList().getItems().size()));
        mainGUI.getMainController().getLabel_playList().textProperty().bind(message);

        // 刷新状态栏
        refreshStatusBar(info);
    }

    /**
     * 弹出播放列表和不弹出播放列表
     */
    public void playList() {
        listOn = !listOn;
        if (listOn) {
            mainGUI.getMainPane().setCenter(null);
            mainGUI.getMainPane().setCenter(mainGUI.getPlayList());
        } else {
            mainGUI.getMainPane().setCenter(null);
            mainGUI.getMainPane().setCenter(mainGUI.getMyMusicPage());
        }
    }

    /**
     * 单击了主界面的下一首按钮后产生的动作
     */
    public void nextPlay() {

        // 获取当前播放歌曲的位置
        int index = playState.getCurrentIndex();
        // 声明音乐
        Music music;
        // 声明扩展信息
        ExtendedInfo info;

        // 如果当前播放状态里的歌曲列表为空则无事发生
        if (playState.getCurrent_songList().size() == 0) return;

        // 判断是否是最后一位
        if (index >= playState.getCurrent_songList().size()) {

            // 如果是最后一位，那么单击按钮后会跳到第一首歌曲
            playState.setCurrentIndex(0);

            music = playState.getCurrent_songList().get(0);
            info = GetMusicInfo.getExtendedInfo(music.getPath());

            playState.setCurrentMusic(music);
            // 开始播放
            player = new MusicMediaPlayer(music);
            player.start();
            refreshStatusBar(info);
        } else {

            //
            if ((index + 1) >= playState.getCurrent_songList().size()) {

                // 播放第一位的音乐
                music = playState.getCurrent_songList().get(0);
                // 获取扩展信息
                info = GetMusicInfo.getExtendedInfo(music.getPath());

                playState.setCurrentMusic(music);
                // 获取播放器
                player = new MusicMediaPlayer(music);
                // 设置当前播放位置为 0
                playState.setCurrentIndex(0);
                // 开始播放
                player.start();
                // 刷新状态栏
                refreshStatusBar(info);
            } else {
                // 获取下一位置的音乐
                music = playState.getCurrent_songList().get(index + 1);
                // 获取扩展信息
                info = GetMusicInfo.getExtendedInfo(music.getPath());

                playState.setCurrentMusic(music);
                // 获取播放器
                player = new MusicMediaPlayer(music);
                // 设置当前播放位置为 index + 1
                playState.setCurrentIndex(index + 1);
                player.start();
                // 刷新状态栏信息
                refreshStatusBar(info);
            }
        }

    }

    /**
     * 单击了主界面的上一首按钮后产生的动作
     */
    public void prePlay() {

        // 获取当前播放歌曲的位置
        int index = playState.getCurrentIndex();
        // 声明音乐
        Music music;
        // 声明扩展信息
        ExtendedInfo info;

        // 如果播放状态里的歌曲列表为空则无事发生
        if (playState.getCurrent_songList().size() == 0) return;

        // 如果播放列表只有一首歌，那么单击就是继续播放这首歌
        if (playState.getCurrent_songList().size() == 1) {
            // 获取音乐
            music = playState.getCurrent_songList().get(index);
            // 获取扩展信息
            info = GetMusicInfo.getExtendedInfo(music.getPath());

            // 设置当前播放的音乐
            playState.setCurrentMusic(music);
            player = new MusicMediaPlayer(music);
            // 设置当前播放位置为 0
            playState.setCurrentIndex(0);
            // 开始播放
            player.start();

            // 刷新状态栏信息
            refreshStatusBar(info);
        } else {
            // 如果播放列表不只是一首歌
            if (index == 0) {
                // 如果是第一首歌，单击上一首还是播放这首歌

                // 获取音乐
                music = playState.getCurrent_songList().get(0);
                // 获取扩展信息
                info = GetMusicInfo.getExtendedInfo(music.getPath());

                // 设置当前播放的音乐
                playState.setCurrentMusic(music);
                player = new MusicMediaPlayer(music);
                // 设置当前播放位置为 0
                playState.setCurrentIndex(0);
                // 开始播放
                player.start();

                // 刷新状态栏信息
                refreshStatusBar(info);
            } else {
                // 如果不是第一首歌，单击上一首就是播放上一首歌

                // 获取音乐
                music = playState.getCurrent_songList().get(index - 1);
                // 获取扩展信息
                info = GetMusicInfo.getExtendedInfo(music.getPath());

                // 设置播放位置
                playState.setCurrentIndex(index - 1);
                // 设置当前播放音乐
                playState.setCurrentMusic(music);
                player = new MusicMediaPlayer(music);
                // 开始播放
                player.start();

                // 刷新状态栏信息
                refreshStatusBar(info);
            }
        }
    }

    /**
     * 播放在我的音乐里的全部音乐
     */
    public void playAll() {
        // 如果我的音乐里没有一首歌，则无事发生
        if (mainGUI.getMyMusicPageController().getTableView_songList().getItems().size() == 0) return;

        // 清除播放列表里的歌曲
        mainGUI.getPlayListController().getTableView_songList().getItems().clear();
        playState.getCurrent_songList().clear();

        // 将我的音乐列表里的歌曲全部添加到播放列表中
        mainGUI.getPlayListController().getTableView_songList().getItems().addAll(mainGUI.getMyMusicPageController().getTableView_songList().getItems());
        playState.getCurrent_songList().addAll(mainGUI.getMyMusicPageController().getTableView_songList().getItems());
        // 获得播放器然后播放

        // 获得第一首歌
        Music firstMusic = mainGUI.getMyMusicPageController().getTableView_songList().getItems().get(0);
        // 获得扩展信息
        ExtendedInfo info = GetMusicInfo.getExtendedInfo(firstMusic.getPath());

        // 设置当前播放的歌曲
        playState.setCurrentMusic(firstMusic);
        // 开始播放
        MusicMediaPlayer player = new MusicMediaPlayer(firstMusic);
        player.start();


        // 将字符串协议和主界面的播放列表内歌曲数目绑定，
        // 每次播放都会更新
        message.set(String.valueOf(mainGUI.getPlayListController().getTableView_songList().getItems().size()));
        mainGUI.getMainController().getLabel_playList().textProperty().bind(message);

        // 刷新状态栏
        refreshStatusBar(info);
    }

    /**
     * 改变播放模式
     */
    public void changeMode() {
        // 获取播放模式
        int currentMode = playState.getCurrentMode();

        // 如果是列表循环，切换为随机播放
        if (currentMode == PlayState.LOOPPLAY) {
            System.out.println("RANDOMPLAY");
            playState.setCurrentMode(PlayState.RANDOMPLAY);
            mainGUI.getMainController().getButton_modeSwitch().getStyleClass().set(0, "modeRandomPlay");
        }

        // 如果是随机播放，切换为单曲循环
        if (currentMode == PlayState.RANDOMPLAY) {
            System.out.println("SINFLECYCLE");
            playState.setCurrentMode(PlayState.SINFLECYCLE);
            mainGUI.getMainController().getButton_modeSwitch().getStyleClass().set(0, "modeSingleCycle");
        }

        // 如果是单曲循环，切换为列表循环
        if (currentMode == PlayState.SINFLECYCLE) {
            System.out.println("LOOPPLAY");
            playState.setCurrentMode(PlayState.LOOPPLAY);
            mainGUI.getMainController().getButton_modeSwitch().getStyleClass().set(0, "modeLoopPlay");
        }
    }

    /**
     * 从本地添加音乐文件
     */
    public void addLocalMusic() {

        // 生成一个文件选择器
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("打开音乐文件");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("MP3", "*.mp3"),
                new FileChooser.ExtensionFilter("所有文件", "*.*"));

        List<File> selectedFile = fileChooser.showOpenMultipleDialog(Main.mainStage);
        List<Music> musicList = new ArrayList<>();

        // 如果选择中了文件， 将文件添加到 ArrayList 中
        if (selectedFile != null) {
            for (File file : selectedFile) {
                Song song = Song.addSong(file.getAbsolutePath());
                Music music = Song.songToMusic(song);
                musicList.add(music);
            }
        }

        // 更新 我的音乐界面的 TableView 信息
        TableView<Music> tableView = mainGUI.getMyMusicPageController().getTableView_songList();
        for (Music music : musicList)
            if (!tableView.getItems().contains(music))
                tableView.getItems().add(music);
        //tableView.getItems().addAll(FXCollections.observableArrayList(musicList));
    }

    /**
     * 从本地添加音乐文件夹
     */
    public void addLocalMusicFolder() {

        // 生成一个文件夹选择器
        DirectoryChooser folderChooser = new DirectoryChooser();
        // 设置文件夹选择器的显示背景
        File selectedFile = folderChooser.showDialog(Main.mainStage);
        List<Music> musicList = new ArrayList<>();


        // 如果选择中了文件夹， 将文件夹的每一个文件都添加到 ArrayList 中
        if (selectedFile != null) {
            ArrayList<File> fileList = new ArrayList<>();
            iterationFolder(selectedFile, fileList);

            // 将音乐文件添加到链表中
            for (File file : fileList) {
                Song song = Song.addSong(file.getAbsolutePath());
                Music music = Song.songToMusic(song);

                musicList.add(music);
            }
        }

        // 更新 我的音乐界面的 TableView 信息
        TableView<Music> tableView = mainGUI.getMyMusicPageController().getTableView_songList();

        for (Music music : musicList)
            if (!tableView.getItems().contains(music))
                tableView.getItems().add(music);

        //tableView.getItems().addAll(FXCollections.observableArrayList(musicList));
    }

    /**
     * 递归文件夹，搜索文件里的音乐文件
     *
     * @param file     需要搜索的（文件 / 文件夹）
     * @param fileList 需要添加到的 ArrayList
     */
    private void iterationFolder(File file, ArrayList<File> fileList) {
        for (File files : file.listFiles()) {
            if (files.isDirectory())
                iterationFolder(files, fileList);
            if (files.isFile()) {
                String name = files.getName();

                // 使用正则表达式来确定是音乐文件
                // *意思是起点，$意思是终点
                // (?i)是指后面字母大小写都可以
                if (name.matches(".*(?i)mp3$"))
                    fileList.add(files);
                if (name.matches(".*(?i)flac$"))
                    fileList.add(files);
            }
        }
    }

    /**
     * 根据 TextField 里的关键词来刷新 搜索面板
     *
     * @param keyword TextField 里的关键词
     */
    public void refreshSearch(String keyword) {
        LinkedList<Music> music = new LinkedList<>();

        // 根据关键词 来分析 json 文件
        try {
            // 分析 json 文件 并添加文件到链表中
            LinkedList<Song> songs = analysisJson(keyword);
            for (Song song : songs)
                music.add(Song.songToMusic(song));

        } catch (IOException e) {
            System.out.println(e.getMessage());
            System.out.println("error in refreshSearch");
        }

        // 更新在 搜索界面 中的 Label 和 TableView
        mainGUI.getSearchPageController().getLabel_searchSong().setText("搜索 " + keyword + " 找到 " + music.size() + " 个文件");
        TableView<Music> tableView = mainGUI.getSearchPageController().getTableView_searchList();
        tableView.getItems().clear();
        tableView.getItems().addAll(FXCollections.observableList(music));
    }

    /**
     * 分析 json 文件
     *
     * @param keyword 关键词
     * @return 分析完 json 文件后将数据填充完成的链表
     * @throws IOException 文件异常
     */
    public LinkedList<Song> analysisJson(String keyword) throws IOException {
        LinkedList<Song> songList = new LinkedList<>();

        System.out.println(keyword);

        // 文件解析工具
        JsonParser parser = new JsonParser();
        JsonArray array = parser.parse(new FileReader("C:\\Users\\hasee\\Documents\\JavaCode\\JavaFxMusicPlayer\\" + keyword + ".json")).getAsJsonArray();

        // 分析数据后填充到链表中
        for (int i = 0; i < array.size(); i++) {
            // 声明歌曲类和标签类
            Song song = new Song();
            Tag tag = new Tag();

            // 获得 Json 里的对象
            JsonObject object = array.get(i).getAsJsonObject();

            // 设置标签信息
            tag.setSongName(object.get("tag").getAsJsonObject().get("songName").getAsString());
            tag.setArtist(object.get("tag").getAsJsonObject().get("artist").getAsString());
            tag.setAlbum(object.get("tag").getAsJsonObject().get("album").getAsString());
            tag.setLength(object.get("tag").getAsJsonObject().get("length").getAsString());
            // 设置标签
            song.setTag(tag);
            song.setPath(object.get("path").getAsString());

            // 添加到音乐表中
            songList.add(song);
        }

        return songList;
    }

    /**
     * 通过网络接收文件
     *
     * @param socket 套接字
     * @throws IOException 网络异常
     */
    public static void receiveFile(Socket socket) throws IOException {

        // 从网络套接字获得文件输入流
        DataInputStream dis = new DataInputStream(socket.getInputStream());

        // 读取文件名
        String read = dis.readUTF();
        // 分析文件
        String[] temp = read.split("/");
        String fileName = temp[0];

        System.out.println("Receive file name: " + fileName + " file length: " + temp[1]);

        String filePath = new File("").getAbsolutePath();


        File file = new File(filePath + "/" + fileName);
        if (!file.exists()) file.createNewFile();

        // 声明文件输出流
        FileOutputStream fos = new FileOutputStream(file);
        // 收到的文件数据（最大30MB）
        byte[] receivedData = new byte[31457280];

        // 从网络输入流读完整地读入文件字节数据
        dis.readFully(receivedData, 0, Integer.valueOf(temp[1]));
        // 输出流写文件
        fos.write(receivedData, 0, Integer.valueOf(temp[1]));
        fos.flush();
        // 关闭文件输出流
        fos.close();

        // 说明文件的保存位置
        Platform.runLater(() -> {
            if (fileName.endsWith("json")) return;
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("通知");
            alert.setContentText("文件已保存至：" + filePath);
            alert.show();
        });
    }

    /**
     * 刷新播放列表标签信息
     */
    public static void refreshPlayList() {
        // 刷新信息
        message.set(String.valueOf(mainGUI.getPlayListController().getTableView_songList().getItems().size()));
        mainGUI.getMainController().getLabel_playList().textProperty().bind(message);
    }

    /**
     * 刷新状态栏信息
     *
     * @param info 扩展信息
     */
    public static void refreshStatusBar(ExtendedInfo info) {

        // 刷新信息
        mainGUI.getMainController().getLabel_BitRate().setText(info.getBitRate());
        mainGUI.getMainController().getLabel_channels().setText(info.getChannels());
        mainGUI.getMainController().getLabel_SampleRate().setText(info.getSampleRate());
        mainGUI.getMainController().getLabel_encodingType().setText(info.getEncodingType());
        mainGUI.getMainController().getLabel_format().setText(info.getFormat());
        mainGUI.getMainController().getLabel_genre().setText(info.getGenre());
        mainGUI.getMainController().getLabel_copyright().setText(info.getCopyright());
        mainGUI.getMainController().getLabel_size().setText(info.getSize());
    }

    /**
     * 列表是否打开
     *
     * @return true 说明打开
     */
    public boolean isListOn() {
        return listOn;
    }

    /**
     * 获取 message 的信息
     *
     * @return 返回 message 的信息
     */
    public static String getMessage() {
        return message.get();
    }

    /**
     * 获取字符串属性
     *
     * @return 返回字符串属性
     */
    public static StringProperty messageProperty() {
        return message;
    }
}

