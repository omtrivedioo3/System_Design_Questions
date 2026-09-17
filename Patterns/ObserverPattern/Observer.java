package Patterns.ObserverPattern;

import java.util.HashSet;
import java.util.Set;

// 1. Subject Interface
interface Channel {
  String getName();

  void subscribe(Subscriber subscriber);

  void unsubscribe(Subscriber subscriber);

  void notifySubscribers(String videoTitle);

  void upload(String videoTitle);
}

// 2. Concrete Subject
class YoutubeChannel implements Channel {
  private final String name;
  private final Set<Subscriber> subscribers = new HashSet<>(); // Prevents duplicate subscriptions

  public YoutubeChannel(String name) {
    this.name = name;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public void subscribe(Subscriber subscriber) {
    subscribers.add(subscriber);
  }

  @Override
  public void unsubscribe(Subscriber subscriber) {
    subscribers.remove(subscriber);
  }

  @Override
  public void upload(String videoTitle) {
    System.out.println("[" + name + "] Uploading new video: " + videoTitle);
    notifySubscribers(videoTitle);
  }

  @Override
  public void notifySubscribers(String videoTitle) {
    for (Subscriber subscriber : subscribers) {
      subscriber.update(this.name, videoTitle);
    }
  }
}

// 3. Observer Interface
interface Subscriber {
  void update(String channelName, String videoTitle);
}

// 4. Concrete Observer
class User implements Subscriber {
  private final String name; // Properly encapsulated, decoupled from Channel

  public User(String name) {
    this.name = name;
  }

  @Override
  public void update(String channelName, String videoTitle) {
    System.out.println("Hello " + name + ", [" + channelName + "] uploaded: " + videoTitle);
  }
}

public class Observer {
  public static void main(String[] args) {
    Channel channel = new YoutubeChannel("omChannel");
    Subscriber user1 = new User("om");
    Subscriber user2 = new User("Gaurav");
    Subscriber user3 = new User("Aarav");

    channel.subscribe(user1);
    channel.subscribe(user2);
    channel.subscribe(user3);

    channel.upload("Video 1");
  }
}
