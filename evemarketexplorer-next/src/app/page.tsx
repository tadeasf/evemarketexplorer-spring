'use client';

import React, { useState } from 'react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { ItemSearch } from '@/components/ItemSearch';
import { MarketData } from '@/components/MarketData';
import { Badge } from '@/components/ui/badge';
import { TrendingUp, Search, Database } from 'lucide-react';
import type { ItemType } from '@/types/api';

export default function Home() {
  const [selectedItem, setSelectedItem] = useState<ItemType | null>(null);

  const handleItemSelect = (item: ItemType) => {
    setSelectedItem(item);
  };

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 to-slate-100 dark:from-slate-950 dark:to-slate-900">
      <div className="container mx-auto px-4 py-8">
        {/* Header */}
        <div className="text-center mb-12">
          <div className="flex items-center justify-center gap-3 mb-4">
            <div className="p-3 rounded-full bg-primary/10">
              <TrendingUp className="h-8 w-8 text-primary" />
            </div>
            <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-900 to-slate-600 dark:from-slate-100 dark:to-slate-400 bg-clip-text text-transparent">
              EVE Market Explorer
            </h1>
          </div>
          <p className="text-xl text-muted-foreground mb-6">
            Discover market opportunities across New Eden
          </p>
          <div className="flex items-center justify-center gap-2 mb-8">
            <Badge variant="secondary">Real-time Data</Badge>
            <Badge variant="secondary">All Regions</Badge>
            <Badge variant="secondary">Advanced Analytics</Badge>
          </div>
        </div>

        {/* Search Section */}
        {!selectedItem ? (
          <div className="max-w-2xl mx-auto">
            <Card className="shadow-lg border-0 bg-white/50 dark:bg-slate-900/50 backdrop-blur-sm">
              <CardHeader className="text-center">
                <CardTitle className="flex items-center justify-center gap-2">
                  <Search className="h-5 w-5" />
                  Search Items
                </CardTitle>
                <CardDescription>
                  Find any tradeable item in EVE Online and explore its market data
                </CardDescription>
              </CardHeader>
              <CardContent className="space-y-6">
                <ItemSearch
                  onItemSelect={handleItemSelect}
                  placeholder="Search for ships, modules, materials..."
                  className="w-full"
                />
                
                {/* Quick Stats or Popular Items */}
                <div className="grid grid-cols-1 sm:grid-cols-3 gap-4 pt-6 border-t">
                  <div className="text-center">
                    <Database className="h-8 w-8 mx-auto mb-2 text-blue-500" />
                    <p className="text-sm font-medium">Live Market Data</p>
                    <p className="text-xs text-muted-foreground">Updated hourly</p>
                  </div>
                  <div className="text-center">
                    <TrendingUp className="h-8 w-8 mx-auto mb-2 text-green-500" />
                    <p className="text-sm font-medium">Price Analytics</p>
                    <p className="text-xs text-muted-foreground">Trends & insights</p>
                  </div>
                  <div className="text-center">
                    <Search className="h-8 w-8 mx-auto mb-2 text-purple-500" />
                    <p className="text-sm font-medium">Smart Search</p>
                    <p className="text-xs text-muted-foreground">Find anything fast</p>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        ) : (
          /* Market Data Section */
          <div className="space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-4">
                <h2 className="text-2xl font-bold">Market Analysis</h2>
                <Badge variant="outline">{selectedItem.name}</Badge>
              </div>
              <button
                onClick={() => setSelectedItem(null)}
                className="text-muted-foreground hover:text-foreground transition-colors"
              >
                ← Back to Search
              </button>
            </div>
            
            <MarketData item={selectedItem} />
          </div>
        )}
      </div>

      {/* Footer */}
      <footer className="border-t mt-16 py-8 text-center text-sm text-muted-foreground">
        <div className="container mx-auto px-4">
          <p>
            EVE Market Explorer - Built with EVE Online ESI API | 
            <a href="https://www.eveonline.com/" className="hover:underline ml-1" target="_blank" rel="noopener noreferrer">
              EVE Online
            </a>
          </p>
        </div>
      </footer>
    </div>
  );
}
