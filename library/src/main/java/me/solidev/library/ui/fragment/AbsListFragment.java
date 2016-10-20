package me.solidev.library.ui.fragment;

import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

import me.solidev.library.R;
import me.solidev.library.ui.adapter.Item;
import me.solidev.library.ui.adapter.MultiTypeAdapter;
import me.solidev.library.ui.adapter.wrapper.HeaderAndFooterWrapper;
import me.solidev.library.ui.adapter.wrapper.LoadMoreWrapper;
import me.solidev.library.ui.widget.StatusViewLayout;
import me.solidev.library.ui.widget.pulltorefresh.PullToRefresh;
import me.solidev.library.utils.ToastUtil;

/**
 * Created by _SOLID
 * Date:2016/9/28
 * Time:15:02
 * Desc:列表基类，默认线性布局
 */

public abstract class AbsListFragment<E extends Item> extends BaseFragment implements IList {

    private StatusViewLayout mStatusViewLayout;
    private PullToRefresh mPullToRefresh;
    private RecyclerView mRecyclerView;

    private HeaderAndFooterWrapper mHeaderAndFooterWrapper;
    private LoadMoreWrapper mLoadMoreWrapper;
    private int mCurrentPageIndex;
    private List<E> mItems;
    private boolean mIsCanPullUp = false;


    @Override
    protected final int setLayoutResourceID() {
        return R.layout.lib_fragment_base_recyclerview;
    }

    @Override
    protected final void init() {
        mCurrentPageIndex = getInitPageIndex();
        mItems = new ArrayList<>();
        mHeaderAndFooterWrapper = new HeaderAndFooterWrapper(getAdapter());
        mLoadMoreWrapper = new LoadMoreWrapper(getContext(), mHeaderAndFooterWrapper);
        mLoadMoreWrapper.setOnLoadListener(new LoadMoreWrapper.OnLoadListener() {
            @Override
            public void onRetry() {
                loadData(mCurrentPageIndex);
            }

            @Override
            public void onLoadMore() {
                AbsListFragment.this.loadMore();
            }
        });
    }

    @Override
    protected final void setUpView() {
        mStatusViewLayout = $(R.id.status_view_layout);
        mPullToRefresh = $(R.id.ptr);
        mPullToRefresh.setPullUpEnable(false);
        disEnablePullUp();
        mRecyclerView = $(R.id.recyclerview);
        mRecyclerView.setLayoutManager(getLayoutManager());
        mRecyclerView.setAdapter(mLoadMoreWrapper);
        customConfig();
        mPullToRefresh.setListener(new PullToRefresh.OnRefreshListener() {
            @Override
            public void onRefresh() {
                refreshData();
            }

            @Override
            public void onLoadMore() {
                loadMore();
            }
        });

        mStatusViewLayout.setOnRetryListener(new View.OnClickListener() {//错误重试
            @Override
            public void onClick(View v) {
                mStatusViewLayout.showLoading();
                loadData(getInitPageIndex());
            }
        });


    }


    @Override
    protected final void setUpData() {
        showLoading();
        loadData(getInitPageIndex());//初始加载首页数据
    }


    @Override
    public final void refreshData() {
        mCurrentPageIndex = getInitPageIndex();
        mLoadMoreWrapper.showLoadMore();
        loadData(getInitPageIndex());
    }

    @Override
    public final void loadMore() {
        loadData(++mCurrentPageIndex);
    }

    @Override
    public abstract void loadData(int pageIndex);

    //region 可直接调用的方法

    /**
     * 列表数据接收成功时调用（相关的实现类需要手动去调用此方法）
     *
     * @param pageIndex 当前请求的页数
     * @param items     返回的数据
     */
    protected final void onDataSuccessReceived(int pageIndex, List<E> items) {
        showContent();
        if (pageIndex == getInitPageIndex() && items.size() <= 0) {//无数据
            showEmpty(getEmptyMsg());
        } else if (pageIndex == getInitPageIndex()) {//刷新
            mItems.clear();
            mItems.addAll(items);
        } else if (items != null && items.size() != 0) {//加载更多
            mItems.addAll(items);
        } else {//没有更多数据了
            mCurrentPageIndex--;
            mPullToRefresh.setPullUpEnable(false);
            mLoadMoreWrapper.showLoadComplete();
        }

        mLoadMoreWrapper.notifyDataSetChanged();

    }


    /**
     * 得到当前列表数据
     *
     * @return 当前列表数据
     */
    protected final List<E> getItems() {
        return mItems;
    }

    /**
     * 添加分隔线
     *
     * @param itemDecoration 分隔线
     */
    protected final void addItemDecoration(RecyclerView.ItemDecoration itemDecoration) {
        if (mRecyclerView != null)
            mRecyclerView.addItemDecoration(itemDecoration);
    }

    /**
     * 禁掉上拉加载更多
     */
    protected final void disEnablePullUp() {
        mIsCanPullUp = false;
        mPullToRefresh.setPullUpEnable(false);
    }


    /**
     * 添加headerView，建议在onDataSuccessReceived方法之前调用
     *
     * @param view headView
     */
    protected final void addHeaderView(View view) {
        mHeaderAndFooterWrapper.addHeaderView(view);
    }
    //endregion

    //region 根据具体的情况可选择性实现下面方法

    protected void customConfig() {

    }

    protected int getInitPageIndex() {
        return 1;
    }

    protected MultiTypeAdapter getAdapter() {
        return new MultiTypeAdapter(mItems);
    }

    @NonNull
    protected RecyclerView.LayoutManager getLayoutManager() {
        return new LinearLayoutManager(getContext());
    }

    @NonNull
    protected String getEmptyMsg() {
        return "无数据";
    }

    protected View getNoMoreView() {
        return LayoutInflater.from(getContext()).inflate(R.layout.lib_layout_footer_view, mStatusViewLayout, false);
    }
    //endregion

    //region 数据加载状态的处理
    @Override
    public void showError(Exception e) {
        if (mCurrentPageIndex == getInitPageIndex()) {
            mStatusViewLayout.showError(e.getMessage());
        } else {
            mLoadMoreWrapper.showLoadError();
            ToastUtil.getInstance().showShortToast(e.getMessage());
        }
        mPullToRefresh.onFinishLoading();
    }

    @Override
    public void showEmpty(String msg) {
        mStatusViewLayout.showEmpty(msg);
        mPullToRefresh.onFinishLoading();
    }

    @Override
    public void showLoading() {
        mStatusViewLayout.showLoading();
    }

    @Override
    public void showContent() {
        mStatusViewLayout.showContent();
        mPullToRefresh.onFinishLoading();
    }
    //endregion
}
