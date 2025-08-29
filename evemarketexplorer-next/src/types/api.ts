export interface ItemGroup {
  groupId: number;
  name: string;
  categoryId: number;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ItemCategory {
  categoryId: number;
  name: string;
  published: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface ItemType {
  typeId: number;
  name: string;
  description?: string;
  group: ItemGroup;
  published: boolean;
  mass?: number;
  volume?: number;
  capacity?: number;
  portionSize?: number;
  basePrice?: number;
  marketGroupId?: number;
  createdAt: string;
  updatedAt: string;
}

export interface Region {
  regionId: number;
  name: string;
  description?: string;
  createdAt: string;
  updatedAt: string;
}

export interface SolarSystem {
  systemId: number;
  name: string;
  regionId: number;
  constellationId: number;
  security: number;
  createdAt: string;
  updatedAt: string;
}

export interface MarketOrder {
  orderId: number;
  region: Region;
  itemType: ItemType;
  locationId: number;
  locationName: string; // Resolved location name
  systemId?: number;
  systemName?: string; // Resolved system name
  isBuyOrder: boolean;
  price: number;
  volumeTotal: number;
  volumeRemain: number;
  minVolume: number;
  duration: number;
  issued: string;
  createdAt: string;
  updatedAt: string;
  dataState: 'LATEST' | 'STAGING';
}

export interface MarketOrdersResponse {
  sell_orders?: MarketOrder[];
  buy_orders?: MarketOrder[];
}

export interface MarketSummary {
  totalOrders: number;
  totalVolume: number;
  averagePrice: number;
  medianPrice: number;
  lowestSellPrice?: number;
  highestBuyPrice?: number;
  priceSpread?: number;
}

export interface SystemHealthResponse {
  status: string;
  timestamp: number;
  esi: {
    rateLimited: boolean;
    remainingErrorBudget: number;
    availableConnections: number;
  };
}

export interface SystemStatusResponse {
  initialized: boolean;
  hasMarketData: boolean;
  timestamp: number;
  data: {
    regions: number;
    itemCategories: number;
    itemGroups: number;
    itemTypes: number;
    marketOrders: number;
  };
  status: 'INITIALIZING' | 'UNIVERSE_DATA_READY' | 'FULLY_OPERATIONAL' | 'UNKNOWN';
}

export interface ApiError {
  status: string;
  message: string;
  timestamp?: number;
}