#
# Sonar, entreprise quality control tool.
# Copyright (C) 2008-2012 SonarSource
# mailto:contact AT sonarsource DOT com
#
# Sonar is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# Sonar is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public
# License along with Sonar; if not, write to the Free Software
# Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
#
require 'set'
class MeasureFilterDisplayList < MeasureFilterDisplay
  KEY = :list

  class Column
    attr_reader :key, :metric, :period

    def initialize(key)
      @key = key
      fields = @key.split(':')
      if fields.size>=2 && fields[0]=='metric'
        @metric = Metric.by_key(fields[1])
        @period = fields[2].to_i if fields.size>=3
      end
    end

    def title_label
      if @metric
        label = Api::Utils.message("measure_filter.short_col.metric.#{@metric.key}", :default => '')
        label = Api::Utils.message("metric.#{@metric.key}.name", :default => @metric.short_name) if label==''
      else
        label = Api::Utils.message("measure_filter.short_col.#{@key}", :default => '')
        label = Api::Utils.message("measure_filter.col.#{@key}", :default => @key) if label==''
      end
      label
    end

    def tooltip
      @metric.description if @metric
    end

    def align
      @align ||=
        begin
          # by default is table cells are left-aligned
          (@key=='name' || @key=='short_name' || @key=='description') ? '' : 'right'
        end
    end

    def title_css
      'thin' if @metric && @metric.val_type==Metric::VALUE_TYPE_LEVEL
    end

    def row_css
      'nowrap' unless @metric && !@metric.numeric?
    end

    def sort?
      !links?
    end

    def links?
      @key == 'links'
    end

  end

  attr_reader :columns

  def initialize(filter, options)
    super(filter, options)

    # default values
    filter.set_criteria_default_value('cols', ['metric:alert_status', 'name', 'date', 'metric:ncloc', 'metric:violations', 'links'])
    filter.set_criteria_default_value('sort', 'name')
    filter.set_criteria_default_value('asc', 'true')
    filter.set_criteria_default_value('pageSize', '30')
    filter.pagination.per_page = [filter.criteria['pageSize'].to_i, 200].min
    filter.pagination.page = (filter.criteria['page'] || 1).to_i

    @columns = []
    metrics = []
    filter.criteria('cols').each do |column_key|
      column = Column.new(column_key)
      @columns << column
      metrics << column.metric if column.metric
      filter.require_links=true if column.links?
    end
    filter.metrics=(metrics)
  end

  PROPERTY_KEYS = Set.new(['cols', 'sort', 'asc', 'pageSize'])

  def url_params
    @filter.criteria.select { |k, v| PROPERTY_KEYS.include?(k) }
  end
end