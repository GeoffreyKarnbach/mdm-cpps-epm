export interface NavigationItem {
    component: string;
    queryParams: QueryParams;
}

export type QueryParams = { [key: string]: string };