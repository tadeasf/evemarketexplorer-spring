import { apiClient } from '@/lib/api';
import type {
  ItemType,
  MarketOrdersResponse,
  MarketSummary,
  Region,
  SystemHealthResponse,
  SystemStatusResponse,
} from '@/types/api';

export class EveMarketApiService {
  // Item endpoints
  static async searchItems(query: string, limit: number = 50): Promise<ItemType[]> {
    const params = new URLSearchParams({
      query,
      limit: limit.toString(),
    });
    return apiClient.get<ItemType[]>('/items/types/search', params);
  }

  static async getAllMarketableItems(): Promise<ItemType[]> {
    return apiClient.get<ItemType[]>('/items/types');
  }

  static async getItemById(typeId: number): Promise<ItemType> {
    return apiClient.get<ItemType>(`/items/types/${typeId}`);
  }

  // Market order endpoints
  static async getMarketOrdersForItem(
    typeId: number,
    orderType: 'buy' | 'sell' | 'all' = 'all',
    limit: number = 200
  ): Promise<MarketOrdersResponse> {
    const params = new URLSearchParams({
      orderType,
      limit: limit.toString(),
    });
    return apiClient.get<MarketOrdersResponse>(`/market/orders/item/${typeId}`, params);
  }

  static async getMarketOrdersForRegionAndItem(
    regionId: number,
    typeId: number,
    orderType: 'buy' | 'sell' | 'all' = 'all',
    limit: number = 100
  ): Promise<MarketOrdersResponse> {
    const params = new URLSearchParams({
      orderType,
      limit: limit.toString(),
    });
    return apiClient.get<MarketOrdersResponse>(
      `/market/regions/${regionId}/orders/item/${typeId}`,
      params
    );
  }

  static async getMarketSummaryForRegion(regionId: number): Promise<MarketSummary> {
    return apiClient.get<MarketSummary>(`/market/regions/${regionId}/summary`);
  }

  // Universe endpoints
  static async getAllRegions(): Promise<Region[]> {
    return apiClient.get<Region[]>('/universe/regions');
  }

  static async getRegionById(regionId: number): Promise<Region> {
    return apiClient.get<Region>(`/universe/regions/${regionId}`);
  }

  // System endpoints
  static async getSystemHealth(): Promise<SystemHealthResponse> {
    return apiClient.get<SystemHealthResponse>('/system/health');
  }

  static async getSystemStatus(): Promise<SystemStatusResponse> {
    return apiClient.get<SystemStatusResponse>('/system/status');
  }

  // Utility methods
  static calculateMarketSummary(orders: MarketOrdersResponse): MarketSummary {
    const sellOrders = orders.sell_orders || [];
    const buyOrders = orders.buy_orders || [];
    const allOrders = [...sellOrders, ...buyOrders];

    if (allOrders.length === 0) {
      return {
        totalOrders: 0,
        totalVolume: 0,
        averagePrice: 0,
        medianPrice: 0,
      };
    }

    const totalOrders = allOrders.length;
    const totalVolume = allOrders.reduce((sum, order) => sum + order.volumeRemain, 0);
    
    // Calculate weighted average price by volume
    const totalValue = allOrders.reduce(
      (sum, order) => sum + (order.price * order.volumeRemain),
      0
    );
    const averagePrice = totalValue / totalVolume;

    // Calculate median price
    const sortedPrices = allOrders.map(order => order.price).sort((a, b) => a - b);
    const medianPrice = sortedPrices.length % 2 === 0
      ? (sortedPrices[sortedPrices.length / 2 - 1] + sortedPrices[sortedPrices.length / 2]) / 2
      : sortedPrices[Math.floor(sortedPrices.length / 2)];

    const lowestSellPrice = sellOrders.length > 0 
      ? Math.min(...sellOrders.map(o => o.price))
      : undefined;
    
    const highestBuyPrice = buyOrders.length > 0 
      ? Math.max(...buyOrders.map(o => o.price))
      : undefined;

    const priceSpread = lowestSellPrice && highestBuyPrice 
      ? lowestSellPrice - highestBuyPrice
      : undefined;

    return {
      totalOrders,
      totalVolume,
      averagePrice,
      medianPrice,
      lowestSellPrice,
      highestBuyPrice,
      priceSpread,
    };
  }
}