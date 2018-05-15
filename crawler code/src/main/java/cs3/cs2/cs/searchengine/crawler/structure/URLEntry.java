package cs3.cs2.cs.searchengine.crawler.structure;


import java.net.MalformedURLException;
import java.net.URL;


public class URLEntry implements Comparable<URLEntry>{
        private URL url;
        private long toCrawlDate;

        public URLEntry(URL url,long toCrawlDate){
            this.url=url;
            this.toCrawlDate = toCrawlDate;
        }

        public URLEntry(String urlString,long toCrawlDate){
            try {
				this.url=new URL(urlString);
			} catch (MalformedURLException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
            this.toCrawlDate = toCrawlDate;
        }

		@Override
		public int compareTo(URLEntry o) {
            URLEntry u = (URLEntry)o;
            
            return (int)(u.getToCrawlDate() - this.toCrawlDate);
        }

		/**
		 * @return the url
		 */
		public URL getUrl() {
			return url;
		}

		/**
		 * @param url the url to set
		 */
		public void setUrl(URL url) {
			this.url = url;
		}

		/**
		 * @return the toCrawlDate
		 */
		public long getToCrawlDate() {
			return toCrawlDate;
		}

		/**
		 * @param toCrawlDate the toCrawlDate to set
		 */
		public void setToCrawlDate(long toCrawlDate) {
			this.toCrawlDate = toCrawlDate;
		}

    }