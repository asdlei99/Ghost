package com.zcy.ghost.vivideo.presenter;

import android.support.annotation.NonNull;

import com.zcy.ghost.vivideo.base.RxPresenter;
import com.zcy.ghost.vivideo.model.bean.Collection;
import com.zcy.ghost.vivideo.model.bean.VideoInfo;
import com.zcy.ghost.vivideo.model.bean.VideoRes;
import com.zcy.ghost.vivideo.model.db.RealmHelper;
import com.zcy.ghost.vivideo.model.net.RetrofitHelper;
import com.zcy.ghost.vivideo.model.net.VideoHttpResponse;
import com.zcy.ghost.vivideo.presenter.contract.VideoInfoContract;
import com.zcy.ghost.vivideo.utils.BeanUtil;
import com.zcy.ghost.vivideo.utils.RxUtil;
import com.zcy.ghost.vivideo.utils.StringUtils;

import org.simple.eventbus.EventBus;

import java.util.concurrent.TimeUnit;

import rx.Observable;
import rx.Subscription;
import rx.functions.Action0;
import rx.functions.Action1;

/**
 * Description: VideoInfoPresenter
 * Creator: yxc
 * date: 2016/9/21 15:35
 */
public class VideoInfoPresenter extends RxPresenter implements VideoInfoContract.Presenter {

    public final static String Refresh_Video_Info = "Refresh_Video_Info";
    final int WAIT_TIME = 200;
    VideoRes result;
    String dataId = "";

    @NonNull
    final VideoInfoContract.View mView;

    public VideoInfoPresenter(@NonNull VideoInfoContract.View addTaskView, VideoInfo videoInfo) {
        mView = StringUtils.checkNotNull(addTaskView);
        mView.setPresenter(this);
        mView.showContent(BeanUtil.VideoInfo2VideoRes(videoInfo, null));
        this.dataId = videoInfo.dataId;
        getDetailData(videoInfo.dataId);
        setCollectState();
    }

    @Override
    public void getDetailData(String dataId) {
        Subscription rxSubscription = RetrofitHelper.getVideoApi().getVideoInfo(dataId)
                .compose(RxUtil.<VideoHttpResponse<VideoRes>>rxSchedulerHelper())
                .compose(RxUtil.<VideoRes>handleResult())
                .subscribe(new Action1<VideoRes>() {
                    @Override
                    public void call(final VideoRes res) {
                        if (res != null) {
                            if (mView.isActive()) {
                                mView.showContent(res);
                                result = res;
                                postData();
                            }
                        }
                    }
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        if (mView.isActive())
                            mView.hidLoading();
                        mView.showError("数据加载失败");
                    }
                }, new Action0() {
                    @Override
                    public void call() {
                        if (mView.isActive())
                            mView.hidLoading();
                    }
                });
        addSubscrebe(rxSubscription);
    }

    private void postData() {
        Subscription rxSubscription = Observable.timer(WAIT_TIME, TimeUnit.MILLISECONDS)
                .compose(RxUtil.<Long>rxSchedulerHelper())
                .subscribe(new Action1<Long>() {
                    @Override
                    public void call(Long aLong) {
                        EventBus.getDefault().post(result, Refresh_Video_Info);
                    }
                });
        addSubscrebe(rxSubscription);
    }

    @Override
    public void collect() {
        if (RealmHelper.getInstance().queryCollectionId(dataId)) {
            RealmHelper.getInstance().deleteCollection(dataId);
            mView.disCollect();
        } else {
            if (result != null) {
                Collection bean = new Collection();
                bean.setId(String.valueOf(dataId));
                bean.setPic(result.pic);
                bean.setTitle(result.title);
                bean.setTime(System.currentTimeMillis());
                RealmHelper.getInstance().insertCollection(bean);
                mView.collected();
            }
        }
    }

    private void setCollectState() {
        if (RealmHelper.getInstance().queryCollectionId(dataId)) {
            mView.collected();
        } else {
            mView.disCollect();
        }
    }
}