import java.util.Iterator;
import java.util.NoSuchElementException;

public class MovieDB{
    private MyLinkedList<Genre> genres;
    
    public MovieDB() {
        this.genres = new MyLinkedList<>();
    }

    public void insert(MovieDBItem item){
        String genre = item.getGenre();
        String title=item.getTitle();
        
        Genre target = findGenre(genre);
        
        if (target==null) {
            Genre newGenre = new Genre(genre);
            newGenre.getMovies().addSorted(title);
            insertGenreSorted(newGenre);
        } else{
            target.getMovies().addSorted(title);
        }}

    public void delete(MovieDBItem item) {
        String genre=item.getGenre();
        String title = item.getTitle();
        
        Genre target=findGenre(genre);
        if (target == null) return;
        target.getMovies().remove(title);
        
        // 마지막 영화가 삭제되면 장르도 제거
        if (target.getMovies().isEmpty()) {
            removeGenre(genre);
        }}

    public MyLinkedList<MovieDBItem> search(String term){
        MyLinkedList<MovieDBItem> results = new MyLinkedList<>();
        
        for (Genre genreNode : genres) {
            String genre = genreNode.getItem();
            for (String title : genreNode.getMovies()) {
                if (title.contains(term)) {
                    results.add(new MovieDBItem(genre, title));
                }
            }
        }
        return results;
    }
    
    public MyLinkedList<MovieDBItem> items(){
        MyLinkedList<MovieDBItem> results=new MyLinkedList<>();
        for (Genre genreNode : genres) {
            String genre=genreNode.getItem();
            for (String title : genreNode.getMovies()) {
                results.add(new MovieDBItem(genre, title));
            }
        }
        
        return results;
    }
    
    private Genre findGenre(String name){
        for (Genre genreNode : genres) {
            if (genreNode.getItem().equals(name))
                return genreNode;
        }
        return null;
    }
    

    private void insertGenreSorted(Genre newGenre) {
        if (genres.isEmpty()) {
            genres.add(newGenre);
            return;}
        Node<Genre> prev = genres.head;
        Node<Genre> curr=genres.head.getNext();
        while (curr != null) {
            Genre node = curr.getItem();
            if (newGenre.compareTo(node) < 0) {
                prev.insertNext(newGenre);
                genres.numItems++;
                return;
            }
            prev=curr;
            curr = curr.getNext();
        }
        prev.insertNext(newGenre);
        genres.numItems++;
    }
    
    private void removeGenre(String name){
        Node<Genre> prev=genres.head;
        Node<Genre> curr = genres.head.getNext();
        while (curr!=null) {
            if (curr.getItem().getItem().equals(name)) {
                prev.removeNext();
                genres.numItems--;
                return;
            }
            prev = curr;
            curr=curr.getNext();
        }
    }
}

class Genre extends Node<String> implements Comparable<Genre> {
    private MovieList movies;
    public Genre(String name) {
        super(name);
        this.movies = new MovieList();
    }
    public MovieList getMovies() {
        return movies;
    }
    
    @Override
    public int compareTo(Genre o) {
        return this.getItem().compareTo(o.getItem());
    }
    @Override
    public int hashCode() {
        return this.getItem().hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj==null) return false;
        if (getClass() != obj.getClass()) return false;
        Genre other = (Genre) obj;
        return this.getItem().equals(other.getItem());
    }
}

class MovieList implements ListInterface<String> {    
    private MyLinkedList<String> movies;
    public MovieList() {
        this.movies=new MyLinkedList<>();
    }

    @Override
    public Iterator<String> iterator() {
        return movies.iterator();
    }

    @Override
    public boolean isEmpty() {
        return movies.isEmpty();
    }

    @Override
    public int size() {
        return movies.size();
    }

    @Override
    public void add(String item) {
        movies.add(item);
    }

    @Override
    public String first() {
        return movies.first();
    }

    @Override
    public void removeAll() {
        movies.removeAll();
    }
    
    // 정렬 유지하며 삽입, 중복 체크
    public void addSorted(String title) {
        for (String movie : movies) {
            if (movie.equals(title)) return;
        }
        
        if (movies.isEmpty()) {
            movies.add(title);
            return;
        }
        
        Node<String> prev = movies.head;
        Node<String> curr=movies.head.getNext();
        
        while (curr!=null) {
            String movie = curr.getItem();
            if (title.compareTo(movie) < 0) {
                prev.insertNext(title);
                movies.numItems++;
                return;
            }
            prev = curr;
            curr=curr.getNext();
        }
        prev.insertNext(title);
        movies.numItems++;
    }
    public void remove(String title) {
        Node<String> prev=movies.head;
        Node<String> curr = movies.head.getNext();
        while (curr != null) {
            if (curr.getItem().equals(title)) {
                prev.removeNext();
                movies.numItems--;
                return;
            }
            prev=curr;
            curr = curr.getNext();
        }
    }
}