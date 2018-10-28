package site.alice.liveman.service;

import com.alibaba.fastjson.JSONObject;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import site.alice.liveman.event.MediaProxyEvent;
import site.alice.liveman.event.MediaProxyEventListener;
import site.alice.liveman.model.VideoInfo;
import site.alice.liveman.mediaproxy.MediaProxyManager;
import site.alice.liveman.mediaproxy.proxytask.MediaProxyTask;
import site.alice.liveman.utils.HttpRequestUtil;

import java.io.*;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class PostBiliDynamicService {

    private static final Logger LOGGER = LoggerFactory.getLogger(PostBiliDynamicService.class);

    private static boolean postDynamic;

    private static String biliCookie;

    @Value("${bili.cookie}")
    public void setBiliCookie(String biliCookie) {
        PostBiliDynamicService.biliCookie = biliCookie;
    }

    @Value("${bili.post.dynamic}")
    public void setPostDynamic(boolean postDynamic) {
        PostBiliDynamicService.postDynamic = postDynamic;
    }

    private static final File   dynamicPostedListFile = new File("dynamicPostedList.txt");
    private static final String DYNAMIC_POST_API      = "https://api.vc.bilibili.com/dynamic_repost/v1/dynamic_repost/repost";
    private static final String DYNAMIC_POST_PARAM    = "dynamic_id=0&type=4&rid=0&content=#Vtuber##%s# 正在直播：%s https://live.bilibili.com/36577&at_uids=&ctrl=[]&csrf_token=a7c224e907966d34ff2c6417bf0a209c";

    static {
        MediaProxyManager.addListener(new MediaProxyEventListener() {
            @Override
            public void onProxyStart(MediaProxyEvent e) {
                List<String> dynamicPostedList;
                try {
                    dynamicPostedList = FileUtils.readLines(dynamicPostedListFile, StandardCharsets.UTF_8);
                } catch (IOException ignore) {
                    dynamicPostedList = new ArrayList<>();
                }
                if (!postDynamic) {
                    return;
                }
                MediaProxyTask mediaProxyTask = e.getMediaProxyTask();
                VideoInfo videoInfo = mediaProxyTask.getVideoInfo();
                if (videoInfo == null) {
                    return;
                }
                if (!dynamicPostedList.contains(mediaProxyTask.getVideoId())) {
                    try {
                        dynamicPostedList.add(mediaProxyTask.getVideoId());
                        FileUtils.writeLines(dynamicPostedListFile, dynamicPostedList);
                        String res = HttpRequestUtil.downloadUrl(new URL(DYNAMIC_POST_API), biliCookie, String.format(DYNAMIC_POST_PARAM, videoInfo.getChannelInfo().getChannelName(), videoInfo.getTitle()), StandardCharsets.UTF_8, null);
                        JSONObject jsonObject = JSONObject.parseObject(res);
                        if (!jsonObject.getString("msg").equals("succ")) {
                            LOGGER.error("发送B站动态失败" + res);
                        }
                    } catch (IOException ex) {
                        LOGGER.error("发送B站动态失败", ex);
                    }
                }
            }
        });
    }
}