export interface SearchResultItem {
  id: string;
  label: string;
  sublabel?: string;
  icon: string;
  route: string[];
}

export interface SearchResults {
  pages: SearchResultItem[];
  clients: SearchResultItem[];
  instances: SearchResultItem[];
}
