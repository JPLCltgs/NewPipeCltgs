package org.cltgs.ytb;

import static org.schabi.newpipe.util.ListHelper.getUrlAndNonTorrentStreams;

import android.content.Context;
import android.os.Environment;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import org.schabi.newpipe.database.feed.model.FeedGroupEntity;
import org.schabi.newpipe.database.subscription.SubscriptionEntity;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.channel.ChannelInfo;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.local.subscription.SubscriptionManager;
import org.schabi.newpipe.util.ExtractorHelper;
import org.schabi.newpipe.util.ListHelper;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import cz.martlin.xspf.playlist.collections.XSPFTracks;
import cz.martlin.xspf.playlist.elements.XSPFFile;
import cz.martlin.xspf.playlist.elements.XSPFTrack;
import cz.martlin.xspf.util.XSPFException;


public class PlaylistSubsCreator extends ListenableWorker {
    Context appContext;
    /**
     * @param appContext   The application {@link Context}
     * @param workerParams Parameters to setup the internal state of this worker
     */
    public PlaylistSubsCreator(@NonNull final Context appContext,
                               @NonNull final WorkerParameters workerParams) {
        super(appContext, workerParams);
        this.appContext = appContext;
        startWork();

    }

    @NonNull
    @Override
    public ListenableFuture startWork() {
        //Get streams (subs)
        final SubscriptionManager subscriptionManager = new SubscriptionManager(appContext);
        final List<SubscriptionEntity> subscriptions = subscriptionManager.
                getSubscriptions(FeedGroupEntity.GROUP_ALL_ID,
                "",
                false).firstElement().blockingGet();

        for (final SubscriptionEntity st : subscriptions) {
            final ChannelInfo channelInfo = ExtractorHelper.getChannelInfo(
                    0, st.getUrl(), true).blockingGet();
            final ArrayList<StreamInfoItem> items =
                    new ArrayList<StreamInfoItem>();

            items.addAll(channelInfo
                    .getRelatedItems());
            Page currentPage = null;
            while (channelInfo.hasNextPage()) {
                currentPage = channelInfo.getNextPage();
                final ListExtractor.InfoItemsPage<StreamInfoItem> moreItems =
                        ExtractorHelper.getMoreChannelItems(0, st.getUrl(),
                                currentPage).blockingGet();
                items.addAll(moreItems.getItems());
                if (items.size() > 40) {
                    break;
                }
            }

            createStreamList(appContext, items,
                    "Subs_"
                    + channelInfo.getName()
                    + ".xspf");
        }


        return new ListenableFuture() {
            @Override
            public void addListener(@NonNull final Runnable listener,
                                    @NonNull final Executor executor) {

            }

            @Override
            public boolean cancel(final boolean b) {
                return false;
            }

            @Override
            public boolean isCancelled() {
                return false;
            }

            @Override
            public boolean isDone() {
                return false;
            }

            @Override
            public Object get() throws ExecutionException, InterruptedException {
                return null;
            }

            @Override
            public Object get(final long l, final TimeUnit timeUnit)
                    throws ExecutionException, InterruptedException,
                    TimeoutException {
                return null;
            }

        };
    }

    private void createStreamList(final Context cxt,
                                  final List<StreamInfoItem> streamList,
                                  final String nFile) {
        final String nameFile = nFile.replaceAll("[^_\\-.0-9a-zA-Z]", "_");
        //Create playlist
        try {
            final XSPFFile file = XSPFFile.create();

            final XSPFTracks tracks = file.playlist().tracks();

            //Adding items to playlist
            for (final StreamInfoItem st : streamList) {
                final String title = st.getName();
                final List<VideoStream> videoStreamsForExternalPlayers =
                        ListHelper.getSortedStreamVideosList(
                                cxt,
                                getUrlAndNonTorrentStreams(
                                        StreamInfo.getInfo(
                                                st.getUrl()
                                        ).getVideoStreams()
                                ),
                                null,
                                false,
                                false);
                final int index = ListHelper.getDefaultResolutionIndex(cxt,
                        videoStreamsForExternalPlayers);
                final String url = StreamInfo.getInfo(st.getUrl()).
                        getVideoStreams().get(index).getContent();

                final XSPFTrack track = tracks.createTrack(URI.create(url), title);
                track.setImage(URI.create(st.getThumbnailUrl()));
                track.setCreator(st.getUploaderName());
                tracks.add(track);
            }

            //Create file
            final File root = new File(Environment.getExternalStorageDirectory()
                    .getPath() + "/Youtube");
            root.mkdirs();
            final File f = new File(root, nameFile);
            file.save(f);
        } catch (final XSPFException e) {
            Log.d(this.getClass().getSimpleName(), "Problem creating playlist");
        } catch (final ExtractionException e) {
            Log.d(this.getClass().getSimpleName(), "Problem getting stream");
        } catch (final IOException e) {
            Log.d(this.getClass().getSimpleName(), "Problem writing file");
        }
    }

}
