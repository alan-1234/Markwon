package ru.noties.markwon.recycler;

import android.support.annotation.NonNull;
import android.util.Log;
import android.util.SparseArray;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import org.commonmark.node.Node;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.noties.markwon.Markwon;

class MarkwonAdapterImpl extends MarkwonAdapter {

    private final SparseArray<Entry<Holder, Node>> entries;
    private final Entry<Holder, Node> defaultEntry;
    private final Reducer reducer;

    private LayoutInflater layoutInflater;

    private Markwon markwon;
    private List<Node> nodes;

    MarkwonAdapterImpl(
            @NonNull SparseArray<Entry<Holder, Node>> entries,
            @NonNull Entry<Holder, Node> defaultEntry,
            @NonNull Reducer reducer) {
        this.entries = entries;
        this.defaultEntry = defaultEntry;
        this.reducer = reducer;
        setHasStableIds(true);
    }

    @Override
    public void setMarkdown(@NonNull Markwon markwon, @NonNull String markdown) {
        setParsedMarkdown(markwon, markwon.parse(markdown));
    }

    @Override
    public void setParsedMarkdown(@NonNull Markwon markwon, @NonNull Node document) {
        setParsedMarkdown(markwon, reducer.reduce(document));
    }

    @Override
    public void setParsedMarkdown(@NonNull Markwon markwon, @NonNull List<Node> nodes) {
        // clear all entries before applying

        defaultEntry.clear();

        for (int i = 0, size = entries.size(); i < size; i++) {
            entries.valueAt(i).clear();
        }

        this.markwon = markwon;
        this.nodes = nodes;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        if (layoutInflater == null) {
            layoutInflater = LayoutInflater.from(parent.getContext());
        }

        final Entry<Holder, Node> entry = viewType == 0
                ? defaultEntry
                : entries.get(viewType);

        return entry.createHolder(layoutInflater, parent);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {

        final Node node = nodes.get(position);
        final int viewType = getNodeViewType(node.getClass());

        final Entry<Holder, Node> entry = viewType == 0
                ? defaultEntry
                : entries.get(viewType);

        entry.bindHolder(markwon, holder, node);
    }

    @Override
    public int getItemCount() {
        return nodes != null
                ? nodes.size()
                : 0;
    }

    @NonNull
    public List<Node> getItems() {
        return nodes != null
                ? Collections.unmodifiableList(nodes)
                : Collections.<Node>emptyList();
    }

    @Override
    public int getItemViewType(int position) {
        return getNodeViewType(nodes.get(position).getClass());
    }

    @Override
    public long getItemId(int position) {
        final Node node = nodes.get(position);
        final int type = getNodeViewType(node.getClass());
        final Entry<Holder, Node> entry = type == 0
                ? defaultEntry
                : entries.get(type);
        return entry.id(node);
    }

    public int getNodeViewType(@NonNull Class<? extends Node> node) {
        // if has registered -> then return it, else 0
        final int hash = node.hashCode();
        if (entries.indexOfKey(hash) > -1) {
            return hash;
        }
        return 0;
    }

    static class BuilderImpl implements Builder {

        private final SparseArray<Entry<Holder, Node>> entries = new SparseArray<>(3);

        private Entry<Holder, Node> defaultEntry;
        private Reducer reducer;

        @NonNull
        @Override
        public <N extends Node> Builder include(
                @NonNull Class<N> node,
                @NonNull Entry<? extends Holder, ? super N> entry) {
            //noinspection unchecked
            entries.append(node.hashCode(), (Entry<Holder, Node>) entry);
            return this;
        }

        @NonNull
        @Override
        public Builder defaultEntry(@NonNull Entry<? extends Holder, ? extends Node> defaultEntry) {
            //noinspection unchecked
            this.defaultEntry = (Entry<Holder, Node>) defaultEntry;
            return this;
        }

        @NonNull
        @Override
        public Builder defaultEntry(int layoutResId) {
            //noinspection unchecked
            this.defaultEntry = (Entry<Holder, Node>) (Entry) new SimpleNodeEntry(layoutResId);
            return this;
        }

        @NonNull
        @Override
        public Builder reducer(@NonNull Reducer reducer) {
            this.reducer = reducer;
            return this;
        }

        @NonNull
        @Override
        public MarkwonAdapter build() {

            if (defaultEntry == null) {
                //noinspection unchecked
                defaultEntry = (Entry<Holder, Node>) (Entry) new SimpleNodeEntry();
            }

            if (reducer == null) {
                reducer = new ReducerImpl();
            }

            return new MarkwonAdapterImpl(entries, defaultEntry, reducer);
        }
    }

    static class ReducerImpl implements Reducer {

        @NonNull
        @Override
        public List<Node> reduce(@NonNull Node root) {

            final List<Node> list = new ArrayList<>();

//            // we will extract all blocks that are direct children of Document
            Node node = root.getFirstChild();
            Node temp;

            while (node != null) {
                list.add(node);
                temp = node.getNext();
                node.unlink();
                node = temp;
            }

            Log.e("NODES", list.toString());

            return list;
        }
    }
}
