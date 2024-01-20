package com.teratail.q_j9negkl91u9mdr;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.*;

import android.annotation.SuppressLint;
import android.os.*;
import android.view.*;
import android.widget.*;

import java.time.format.*;
import java.util.*;

public class MainActivity extends AppCompatActivity {
  private static final String LOG_TAG = "MainActivity";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    MainViewModel viewModel = new ViewModelProvider(this).get(MainViewModel.class);
    viewModel.getException().observe(this, exception -> {
      if(exception != null) Toast.makeText(MainActivity.this, "エラーが発生しました", Toast.LENGTH_LONG).show();
    });

    TextView headerText = findViewById(R.id.headerText);

    Adapter adapter = new Adapter();
    viewModel.getFeed().observe(this, feed -> {
      DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
      headerText.setText(feed != null ? feed.title + "\n" +
              (feed.updated != null ? dateTimeFormatter.format(feed.updated) : "-") : "");
      adapter.setFeedList(feed != null ? feed.entryList : null);
    });

    RecyclerView feedRecyclerView = findViewById(R.id.feedRecyclerView);
    feedRecyclerView.setAdapter(adapter);
    feedRecyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

    Button updateButton = findViewById(R.id.updateButton);
    updateButton.setOnClickListener(v -> viewModel.requestFeed());

    viewModel.requestFeed();
  }

  private static class Adapter extends RecyclerView.Adapter<Adapter.ViewHolder> {
    private List<Feed.Entry> entryList = new ArrayList<>();

    @SuppressLint("NotifyDataSetChanged")
    void setFeedList(List<Feed.Entry> entryList) {
      this.entryList = entryList != null ? entryList : Collections.emptyList();
      notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
      return new ViewHolder(parent);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
      holder.bind(entryList.get(position));
    }

    @Override
    public int getItemCount() {
      return entryList.size();
    }

    private static class ViewHolder extends RecyclerView.ViewHolder {
      private DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM);
      private final TextView text1, text2, text3, text4;
      public ViewHolder(@NonNull ViewGroup parent) {
        super(LayoutInflater.from(parent.getContext()).inflate(R.layout.feedentry_row, parent, false));
        text1 = itemView.findViewById(R.id.text1);
        text2 = itemView.findViewById(R.id.text2);
        text3 = itemView.findViewById(R.id.text3);
        text4 = itemView.findViewById(R.id.text4);
      }
      void bind(Feed.Entry entry) {
        text1.setText("Title: " + entry.title);
        text2.setText("Updates: " + (entry.updated != null ? entry.updated.format(dateTimeFormatter) : "-"));
        text3.setText("Author: " + entry.authorName);
        text4.setText("Content: " + entry.contentText);
      }
    }
  }
}