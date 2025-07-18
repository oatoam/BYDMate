declare namespace AMap {
  class Map {
    constructor(container: string | HTMLElement, opts?: MapOptions);
    add(overlay: any): void;
    remove(overlay: any): void;
    setZoomAndCenter(zoom: number, center: LngLat | number[]): void;
    setCenter(center: LngLat | number[]): void;
    setZoom(zoom: number): void;
    getZoom(): number;
    getCenter(): LngLat;
    destroy(): void;
    setFitView(overlay?: any, immediately?: boolean): void;
    on(event: string, handler: Function): void;
    off(event: string, handler: Function): void;
  }

  interface MapOptions {
    zoom?: number;
    center?: LngLat | number[];
    viewMode?: '2D' | '3D';
    pitch?: number;
    rotation?: number;
    showLabel?: boolean;
    showBuildingBlock?: boolean;
    terrain?: boolean;
    features?: string[];
    layers?: any[];
    zooms?: [number, number];
    crs?: string;
    animateEnable?: boolean;
    doubleClickZoom?: boolean;
    keyboardEnable?: boolean;
    dragEnable?: boolean;
    zoomEnable?: boolean;
    resizeEnable?: boolean;
    jogEnable?: boolean;
    pitchEnable?: boolean;
    rotateEnable?: boolean;
    buildingAnimation?: boolean;
    expandZoomRange?: boolean;
    labelzIndex?: number;
    isHotspot?: boolean;
    defaultCursor?: string;
    touchZoom?: boolean;
    touchZoomCenter?: number;
    showIndoorMap?: boolean;
    indoorMap?: any;
    skyColor?: string;
    mapStyle?: string;
    baseRender?: string;
    customCoords?: any;
    mask?: any;
  }

  class LngLat {
    constructor(lng: number, lat: number);
    getLng(): number;
    getLat(): number;
    equals(obj: LngLat): boolean;
    distance(obj: LngLat): number;
    offset(x: number, y: number): LngLat;
    toString(): string;
  }

  class Marker {
    constructor(opts?: MarkerOptions);
    setMap(map: Map | null): void;
    setOffset(offset: Pixel): void;
    setPosition(position: LngLat | number[]): void;
    setAngle(angle: number): void;
    setAnimation(animation: string): void;
    setClickable(clickable: boolean): void;
    setCursor(cursor: string): void;
    setDraggable(draggable: boolean): void;
    setExtData(extData: any): void;
    setIcon(icon: string | Icon): void;
    setLabel(label: { content: string; offset: Pixel }): void;
    setShadow(shadow: string | Icon): void;
    setTitle(title: string): void;
    setTop(isTop: boolean): void;
    setzIndex(zIndex: number): void;
    getMap(): Map;
    getOffset(): Pixel;
    getPosition(): LngLat;
    getAngle(): number;
    getAnimation(): string;
    getClickable(): boolean;
    getCursor(): string;
    getDraggable(): boolean;
    getExtData(): any;
    getIcon(): string | Icon;
    getLabel(): { content: string; offset: Pixel };
    getShadow(): string | Icon;
    getTitle(): string;
    getTop(): boolean;
    getzIndex(): number;
    on(event: string, handler: Function): void;
    off(event: string, handler: Function): void;
  }

  interface MarkerOptions {
    map?: Map;
    position?: LngLat | number[];
    offset?: Pixel;
    icon?: string | Icon;
    content?: string | HTMLElement;
    title?: string;
    zoom?: number;
    draggable?: boolean;
    cursor?: string;
    animation?: string;
    shadow?: string | Icon;
    label?: { content: string; offset: Pixel };
    extData?: any;
    topWhenClick?: boolean;
    bubble?: boolean;
    zIndex?: number;
    clickable?: boolean;
    angle?: number;
    autoRotation?: boolean;
    shape?: any;
    anchor?: string;
  }

  class Icon {
    constructor(opts?: IconOptions);
  }

  interface IconOptions {
    size?: Size;
    imageOffset?: Pixel;
    image?: string;
    imageSize?: Size;
  }

  class Size {
    constructor(width: number, height: number);
    getWidth(): number;
    getHeight(): number;
    equals(obj: Size): boolean;
    toString(): string;
  }

  class Pixel {
    constructor(x: number, y: number);
    getX(): number;
    getY(): number;
    equals(obj: Pixel): boolean;
    toString(): string;
  }

  class Polyline {
    constructor(opts?: PolylineOptions);
    setMap(map: Map | null): void;
    setPath(path: LngLat[] | number[][]): void;
    setOptions(opts: PolylineOptions): void;
    getMap(): Map;
    getPath(): LngLat[];
    getOptions(): PolylineOptions;
    getLength(): number;
    on(event: string, handler: Function): void;
    off(event: string, handler: Function): void;
  }

  interface PolylineOptions {
    map?: Map;
    path?: LngLat[] | number[][];
    strokeColor?: string;
    strokeOpacity?: number;
    strokeWeight?: number;
    strokeStyle?: 'solid' | 'dashed';
    strokeDasharray?: number[];
    lineJoin?: 'miter' | 'round' | 'bevel';
    lineCap?: 'butt' | 'round' | 'square';
    zIndex?: number;
    extData?: any;
    bubble?: boolean;
    showDir?: boolean;
    dirColor?: string;
    isOutline?: boolean;
    outlineColor?: string;
    borderWeight?: number;
  }

  class Polygon {
    constructor(opts?: PolygonOptions);
    setMap(map: Map | null): void;
    setPath(path: LngLat[] | number[][]): void;
    setOptions(opts: PolygonOptions): void;
    getMap(): Map;
    getPath(): LngLat[];
    getOptions(): PolygonOptions;
    getArea(): number;
    on(event: string, handler: Function): void;
    off(event: string, handler: Function): void;
  }

  interface PolygonOptions {
    map?: Map;
    path?: LngLat[] | number[][];
    strokeColor?: string;
    strokeOpacity?: number;
    strokeWeight?: number;
    fillColor?: string;
    fillOpacity?: number;
    strokeStyle?: 'solid' | 'dashed';
    strokeDasharray?: number[];
    lineJoin?: 'miter' | 'round' | 'bevel';
    lineCap?: 'butt' | 'round' | 'square';
    zIndex?: number;
    extData?: any;
    bubble?: boolean;
    isOutline?: boolean;
    outlineColor?: string;
    borderWeight?: number;
  }

  class Circle {
    constructor(opts?: CircleOptions);
    setMap(map: Map | null): void;
    setCenter(center: LngLat | number[]): void;
    setRadius(radius: number): void;
    setOptions(opts: CircleOptions): void;
    getMap(): Map;
    getCenter(): LngLat;
    getRadius(): number;
    getBounds(): Bounds;
    getOptions(): CircleOptions;
    on(event: string, handler: Function): void;
    off(event: string, handler: Function): void;
  }

  interface CircleOptions {
    map?: Map;
    center?: LngLat | number[];
    radius?: number;
    strokeColor?: string;
    strokeOpacity?: number;
    strokeWeight?: number;
    fillColor?: string;
    fillOpacity?: number;
    strokeStyle?: 'solid' | 'dashed';
    strokeDasharray?: number[];
    zIndex?: number;
    extData?: any;
    bubble?: boolean;
    isOutline?: boolean;
    outlineColor?: string;
    borderWeight?: number;
  }

  class Bounds {
    constructor(southWest: LngLat, northEast: LngLat);
    contains(point: LngLat): boolean;
    getCenter(): LngLat;
    getSouthWest(): LngLat;
    getNorthEast(): LngLat;
    toString(): string;
  }

  class Geocoder {
    constructor(opts?: GeocoderOptions);
    getLocation(address: string, callback: (status: string, result: GeocoderResult) => void): void;
    getAddress(lnglat: LngLat | number[], callback: (status: string, result: GeocoderResult) => void): void;
  }

  interface GeocoderOptions {
    city?: string;
    radius?: number;
    lang?: string;
    batch?: boolean;
    extensions?: 'base' | 'all';
  }

  interface GeocoderResult {
    info: string;
    geocodes: Geocode[];
    regeocode: Regeocode;
  }

  interface Geocode {
    formattedAddress: string;
    province: string;
    city: string;
    citycode: string;
    district: string;
    township: string;
    street: string;
    streetNum: string;
    adcode: string;
    location: LngLat;
    level: string;
  }

  interface Regeocode {
    formattedAddress: string;
    addressComponent: AddressComponent;
    roads: Road[];
    crosses: Cross[];
    pois: Poi[];
    aois: Aoi[];
  }

  interface AddressComponent {
    province: string;
    city: string;
    citycode: string;
    district: string;
    township: string;
    street: string;
    streetNumber: string;
    adcode: string;
    country: string;
    countrycode: string;
    businessAreas: BusinessArea[];
    building: string;
    neighborhood: string;
    province_code: string;
    city_code: string;
    district_code: string;
    towncode: string;
  }

  interface Road {
    id: string;
    name: string;
    direction: string;
    distance: number;
    location: LngLat;
  }

  interface Cross {
    direction: string;
    distance: number;
    location: LngLat;
    first_id: string;
    first_name: string;
    second_id: string;
    second_name: string;
  }

  interface Poi {
    id: string;
    name: string;
    type: string;
    tel: string;
    direction: string;
    distance: number;
    location: LngLat;
    address: string;
    businessArea: string;
    postcode: string;
    website: string;
    email: string;
    province: string;
    city: string;
    citycode: string;
    district: string;
    adcode: string;
    gridcode: string;
    alias: string;
    indoorData: any;
    entrances: any[];
    exits: any[];
    photos: any[];
  }

  interface Aoi {
    id: string;
    name: string;
    adcode: string;
    location: LngLat;
    area: string;
    distance: number;
    type: string;
  }

  class Autocomplete {
    constructor(opts?: AutocompleteOptions);
    search(keyword: string, callback: (status: string, result: AutocompleteResult) => void): void;
    setCity(city: string): void;
    setCityLimit(cityLimit: boolean): void;
  }

  interface AutocompleteOptions {
    type?: string;
    city?: string;
    datasource?: string;
    citylimit?: boolean;
    input?: string | HTMLElement;
    output?: string | HTMLElement;
    outPutDir?: string;
  }

  interface AutocompleteResult {
    info: string;
    tips: Tip[];
  }

  interface Tip {
    name: string;
    district: string;
    adcode: string;
  }

  class PlaceSearch {
    constructor(opts?: PlaceSearchOptions);
    search(keyword: string, callback: (status: string, result: PlaceSearchResult) => void): void;
    searchNearBy(keyword: string, center: LngLat | number[], radius: number, callback: (status: string, result: PlaceSearchResult) => void): void;
    searchInBounds(keyword: string, bounds: Bounds, callback: (status: string, result: PlaceSearchResult) => void): void;
    setCity(city: string): void;
    setPageIndex(pageIndex: number): void;
    setPageSize(pageSize: number): void;
    setLang(lang: string): void;
    setType(type: string): void;
    setExtensions(extensions: 'base' | 'all'): void;
  }

  interface PlaceSearchOptions {
    city?: string;
    citylimit?: boolean;
    children?: number;
    pageIndex?: number;
    pageSize?: number;
    lang?: string;
    type?: string;
    extensions?: 'base' | 'all';
    panel?: string | HTMLElement;
    autoFitView?: boolean;
  }

  interface PlaceSearchResult {
    info: string;
    poiList: PoiList;
    suggestion: Suggestion;
  }

  interface PoiList {
    count: number;
    pageIndex: number;
    pageSize: number;
    pois: Poi[];
  }

  interface Suggestion {
    keywords: string[];
    cities: City[];
  }

  interface City {
    name: string;
    count: number;
    adcode: string;
  }

  class Driving {
    constructor(opts?: DrivingOptions);
    search(points: DrivingPoint[], callback: (status: string, result: DrivingResult) => void): void;
    setPolicy(policy: DrivingPolicy): void;
    setExtensions(extensions: 'base' | 'all'): void;
  }

  interface DrivingOptions {
    map?: Map;
    panel?: string | HTMLElement;
    policy?: DrivingPolicy;
    extensions?: 'base' | 'all';
    hideMarkers?: boolean;
    showTraffic?: boolean;
    isHotspot?: boolean;
  }

  interface DrivingPolicy {}

  interface DrivingPoint {}

  interface DrivingResult {}
}