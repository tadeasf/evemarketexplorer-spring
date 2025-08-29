'use client';

import React, { useState, useEffect } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Skeleton } from '@/components/ui/skeleton';
import { Alert, AlertDescription } from '@/components/ui/alert';
import { TrendingUp, TrendingDown, DollarSign, Package } from 'lucide-react';
import { EveMarketApiService } from '@/services/api';
import { MarketOrderTable } from '@/components/MarketOrderTable';
import type { ItemType, MarketOrdersResponse, MarketSummary } from '@/types/api';

interface MarketDataProps {
  item: ItemType;
  className?: string;
}

export function MarketData({ item, className }: MarketDataProps) {
  const [marketData, setMarketData] = useState<MarketOrdersResponse | null>(null);
  const [summary, setSummary] = useState<MarketSummary | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    const fetchMarketData = async () => {
      setLoading(true);
      setError(null);
      
      try {
        const data = await EveMarketApiService.getMarketOrdersForItem(item.typeId);
        setMarketData(data);
        
        // Calculate summary statistics
        const calculatedSummary = EveMarketApiService.calculateMarketSummary(data);
        setSummary(calculatedSummary);
      } catch (err) {
        console.error('Error fetching market data:', err);
        setError('Failed to fetch market data. Please try again.');
      } finally {
        setLoading(false);
      }
    };

    fetchMarketData();
  }, [item.typeId]);

  const formatPrice = (price: number) => {
    return new Intl.NumberFormat('en-US', {
      style: 'decimal',
      minimumFractionDigits: 2,
      maximumFractionDigits: 2,
    }).format(price);
  };

  const formatVolume = (volume: number) => {
    return new Intl.NumberFormat('en-US').format(volume);
  };

  if (loading) {
    return (
      <div className={`space-y-6 ${className}`}>
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {Array.from({ length: 4 }).map((_, i) => (
            <Card key={i}>
              <CardHeader className="pb-2">
                <Skeleton className="h-4 w-20" />
              </CardHeader>
              <CardContent>
                <Skeleton className="h-6 w-16 mb-1" />
                <Skeleton className="h-3 w-24" />
              </CardContent>
            </Card>
          ))}
        </div>
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
          <Skeleton className="h-96" />
          <Skeleton className="h-96" />
        </div>
      </div>
    );
  }

  if (error) {
    return (
      <Alert className={className}>
        <AlertDescription>{error}</AlertDescription>
      </Alert>
    );
  }

  if (!marketData || !summary) {
    return (
      <Alert className={className}>
        <AlertDescription>No market data available for this item.</AlertDescription>
      </Alert>
    );
  }

  const sellOrders = marketData.sell_orders || [];
  const buyOrders = marketData.buy_orders || [];

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="space-y-2">
        <div className="flex items-center gap-3">
          <h2 className="text-2xl font-bold">{item.name}</h2>
          <Badge variant="secondary">{item.group.name}</Badge>
        </div>
        {item.description && (
          <p className="text-muted-foreground text-sm">{item.description}</p>
        )}
      </div>

      {/* Summary Statistics */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Average Price</CardTitle>
            <DollarSign className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatPrice(summary.averagePrice)} ISK</div>
            <p className="text-xs text-muted-foreground">
              Volume weighted average
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Median Price</CardTitle>
            <TrendingUp className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatPrice(summary.medianPrice)} ISK</div>
            <p className="text-xs text-muted-foreground">
              Middle value of all orders
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Total Volume</CardTitle>
            <Package className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">{formatVolume(summary.totalVolume)}</div>
            <p className="text-xs text-muted-foreground">
              Available units
            </p>
          </CardContent>
        </Card>

        <Card>
          <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
            <CardTitle className="text-sm font-medium">Price Spread</CardTitle>
            <TrendingDown className="h-4 w-4 text-muted-foreground" />
          </CardHeader>
          <CardContent>
            <div className="text-2xl font-bold">
              {summary.priceSpread ? formatPrice(summary.priceSpread) : 'N/A'} ISK
            </div>
            <p className="text-xs text-muted-foreground">
              Sell min - Buy max
            </p>
          </CardContent>
        </Card>
      </div>

      {/* Price Extremes */}
      {(summary.lowestSellPrice || summary.highestBuyPrice) && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {summary.lowestSellPrice && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-green-600">Lowest Sell Price</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-xl font-bold text-green-600">
                  {formatPrice(summary.lowestSellPrice)} ISK
                </div>
              </CardContent>
            </Card>
          )}
          {summary.highestBuyPrice && (
            <Card>
              <CardHeader className="pb-2">
                <CardTitle className="text-sm text-blue-600">Highest Buy Price</CardTitle>
              </CardHeader>
              <CardContent>
                <div className="text-xl font-bold text-blue-600">
                  {formatPrice(summary.highestBuyPrice)} ISK
                </div>
              </CardContent>
            </Card>
          )}
        </div>
      )}

      {/* Order Tables */}
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <span className="text-red-600">Sell Orders</span>
              <Badge variant="outline">{sellOrders.length}</Badge>
            </CardTitle>
            <CardDescription>
              Sorted by lowest price first
            </CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            <MarketOrderTable orders={sellOrders} orderType="sell" />
          </CardContent>
        </Card>

        <Card>
          <CardHeader>
            <CardTitle className="flex items-center gap-2">
              <span className="text-green-600">Buy Orders</span>
              <Badge variant="outline">{buyOrders.length}</Badge>
            </CardTitle>
            <CardDescription>
              Sorted by highest price first
            </CardDescription>
          </CardHeader>
          <CardContent className="p-0">
            <MarketOrderTable orders={buyOrders} orderType="buy" />
          </CardContent>
        </Card>
      </div>
    </div>
  );
}